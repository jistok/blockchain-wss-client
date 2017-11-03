package io.pivotal.dil.blockchain;

import java.net.URI;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 *
 * Data source: https://blockchain.info/api/api_websocket Code this depends on:
 * https://github.com/TooTallNate/Java-WebSocket
 *
 * How to run in separate thread, from Boot app?
 * https://stackoverflow.com/questions/39737013/spring-boot-best-way-to-start-a-background-thread-on-deployment
 * https://stackoverflow.com/questions/37390718/start-thread-at-springboot-application
 *
 * Usage:
 * <pre>
 * public static void main(String[] args) throws Exception {
		final BlockchainWssClient c = new BlockchainWssClient(new URI(BLOCKCHAIN_URL));
		c.connect();
   }
 * </pre>
 *
 */
public class BlockchainWssClient extends WebSocketClient {

	public BlockchainWssClient(URI serverURI) {
		super(serverURI);
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		System.out.println("opened connection");
		this.send("{\"op\":\"unconfirmed_sub\"}");
	}

	@Override
	public void onMessage(String message) {
		try {
			BlockchainWssClientApplication.TXN_QUEUE.put(message);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		System.out.println(message);
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		// The codecodes are documented in class org.java_websocket.framing.CloseFrame
		System.out.println(
				"Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: " + reason);
	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
	}

}
