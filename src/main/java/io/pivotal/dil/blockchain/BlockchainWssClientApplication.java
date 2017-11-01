package io.pivotal.dil.blockchain;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlockchainWssClientApplication {
	
	private static final String BLOCKCHAIN_URL = "wss://ws.blockchain.info/inv";
	
	public static void main(String[] args) {
		BlockchainWssClient c = null;
		try {
			c = new BlockchainWssClient(new URI(BLOCKCHAIN_URL));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		c.connect();
		SpringApplication.run(BlockchainWssClientApplication.class, args);
	}
}
