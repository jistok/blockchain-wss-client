package io.pivotal.dil.blockchain;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gemstone.gemfire.cache.Region;

import io.pivotal.dil.blockchain.entity.BlockchainInput;
import io.pivotal.dil.blockchain.entity.BlockchainItem;
import io.pivotal.dil.blockchain.entity.BlockchainTxn;

@SuppressWarnings("unused")
@Component
public class BlockchainWssServer extends WebSocketServer {

	private static final int DEFAULT_PORT = 18080;

	private static final long TIME_TO_WAIT_BEFORE_RECONNECTING_MS = 5000L;

	private static final long QUEUE_MAX_WAIT_TIME_SECONDS = 2L;

	@Autowired
	private Region<String /* hash */, BlockchainTxn> blockchainTxnRegion;

	@Autowired
	private Region<String /* BlockchainItem.genId() */, BlockchainItem> blockchainItemRegion;

	private BlockchainWssClient wsClient = null;

	public BlockchainWssServer() {
		this(new InetSocketAddress(DEFAULT_PORT));
	}

	public BlockchainWssServer(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		broadcast("new connection: " + handshake.getResourceDescriptor());
		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		String msg = conn + " has disconnected";
		broadcast(msg);
		System.out.println(msg);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		broadcast(message);
		System.out.println(conn + ": " + message);
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		broadcast(message.array());
		System.out.println(conn + ": " + message);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
	}

	private void startWsClient() {
		// Start our Websocket client to pull in transaction data and stick it into
		// TXN_QUEUE
		try {
			System.out.println("startWsClient(): getting new instance and connecting");
			if (wsClient != null) {
				wsClient.close();
				wsClient = null;
			}
			wsClient = new BlockchainWssClient(new URI(BlockchainWssClientApplication.BLOCKCHAIN_URL));
			wsClient.connect();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onStart() {
		System.out.println("Server started!");
		startWsClient(); // Start client up once here
		new Thread(new Runnable() {
			public void run() {
				long nMessages = 0;
				long nMessagesPerReport = 100;
				long lastTimeReported = System.currentTimeMillis();
				while (true) {
					String msg;
					try {
						msg = BlockchainWssClientApplication.TXN_QUEUE.poll(QUEUE_MAX_WAIT_TIME_SECONDS,
								TimeUnit.SECONDS);
						BlockchainTxn txn = null;
						if (msg != null) {
							txn = BlockchainTxn.fromJSON(msg);
							broadcast(msg);
						} else {
							// We don't have data, so we assume our WS client got disconnected
							startWsClient(); // Restart here as it appears it was closed by upstream server
							continue;
						}
						String statusMsg = "No BlockchainTxn";
						if (txn != null) {
							nMessages++;
							statusMsg = "Got a BlockchainTxn (timeAsDate: " + txn.getTimeAsDate().toString() + ")";
							// Persist BlockchainTxn in Gemfire
							blockchainTxnRegion.put(txn.getId(), txn);
							// Persist all the BlockchainItems in Gemfire
							for (BlockchainInput in : txn.getInputs()) {
								BlockchainItem item = in.getPrevOut();
								blockchainItemRegion.put(item.getId(), item);
							}
							for (BlockchainItem item : txn.getOut()) {
								blockchainItemRegion.put(item.getId(), item);
							}
						}
						if (nMessages > 0L && (nMessages % nMessagesPerReport == 0)) {
							long now = System.currentTimeMillis();
							double dtSec = (now - lastTimeReported) / 1.0E+03;
							System.out.printf("Total messages so far: %d\n" + "Message rate: %.3f per second\n",
									nMessages, nMessagesPerReport / dtSec);
							lastTimeReported = now;
						}
						// System.out.println(statusMsg); // DEBUG
						// System.out.println(txn.toJSON()); // DEBUG
					} catch (InterruptedException /* | JsonProcessingException */ e) {
						throw new RuntimeException(e);
					}
				}
			}
		}).start();
	}

}
