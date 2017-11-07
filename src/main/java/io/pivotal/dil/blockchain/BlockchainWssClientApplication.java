package io.pivotal.dil.blockchain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlockchainWssClientApplication {

	private static final String BLOCKCHAIN_URL = "wss://ws.blockchain.info/inv";
	private static final int QUEUE_CAPACITY = 100;

	// This is how transactions get passed from client to server
	protected static final BlockingQueue<String> TXN_QUEUE = new LinkedBlockingDeque<>(QUEUE_CAPACITY);
	
	@SuppressWarnings("unused")
	private BlockchainWssServer wsServer;
	
	public BlockchainWssClientApplication(BlockchainWssServer wsServer) {
		this.wsServer = wsServer;
		wsServer.start();
		System.out.println("Server started on port: " + wsServer.getPort());
	}
	
	public static void main(String[] args) {
		// Start our Websocket client to pull in transaction data and stick it into TXN_QUEUE
		BlockchainWssClient c = null;
		try {
			c = new BlockchainWssClient(new URI(BLOCKCHAIN_URL));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		c.connect();

		// Finally, the Boot app runs
		SpringApplication.run(BlockchainWssClientApplication.class, args);
	}
}
