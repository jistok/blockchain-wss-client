package io.pivotal.dil.blockchain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;

import io.pivotal.dil.blockchain.entity.BlockchainItem;
import io.pivotal.dil.blockchain.entity.BlockchainTxn;

@Configuration
public class GemfireConfiguration {

	@Autowired
	private PropertyConfig propertyConfig;

	private static final Logger LOG = LoggerFactory.getLogger(GemfireConfiguration.class);

	// Create a connection - client/server topology
	@Bean
	public ClientCache cache() {
		LOG.info("creating cache");
		ClientCacheFactory ccf = new ClientCacheFactory();
		String locators = propertyConfig.locators;
		Pattern pat = Pattern.compile("^\\s*([^\\[]+)\\[(\\d+)\\]\\s*$");
		for (String st : locators.split(",")) {
			Matcher mat = pat.matcher(st);
			if (mat.matches()) {
			String host = mat.group(1);
			int port = Integer.parseInt(mat.group(2));
			LOG.info("creating cache: adding locator: host={}, port={}", host, port);
			ccf.addPoolLocator(host, port);
			} else {
				throw new RuntimeException("Unable to match locator HOST[PORT] in \"" + st + "\"");
			}
		}
		ccf.set("security-client-auth-init",
				"io.pivotal.dil.blockchain.ClientAuthInitialize.create");
		ccf.set("security-username", propertyConfig.username);
		ccf.set("security-password", propertyConfig.password);
		ccf.setPdxPersistent(false);
		ccf.setPdxReadSerialized(true); /* REQUIRED if using S3JSONAsyncEventListener */
		ccf.setPdxSerializer(new ReflectionBasedAutoSerializer("io.pivotal.dil.blockchain.entity.*"));
		return ccf.create();
	}

	// Get regions configured as non-caching proxies (data remains remote; a pure
	// client-server topology)
	@Bean
	public Region<String /* hash */, BlockchainTxn> blockchainTxnRegion(ClientCache cache) {
		ClientRegionFactory<String, BlockchainTxn> crf = cache.createClientRegionFactory(ClientRegionShortcut.PROXY);
		return crf.create("BlockchainTxn");
	}

	@Bean
	public Region<String /* BlockchainItem.genId() */, BlockchainItem> blockchainItemRegion(ClientCache cache) {
		ClientRegionFactory<String, BlockchainItem> crf = cache.createClientRegionFactory(ClientRegionShortcut.PROXY);
		return crf.create("BlockchainItem");
	}

}
