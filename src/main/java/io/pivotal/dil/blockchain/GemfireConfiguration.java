package io.pivotal.dil.blockchain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.cache.client.ClientRegionFactory;
import com.gemstone.gemfire.cache.client.ClientRegionShortcut;
import com.gemstone.gemfire.pdx.ReflectionBasedAutoSerializer;

import io.pivotal.dil.blockchain.entity.BlockchainItem;
import io.pivotal.dil.blockchain.entity.BlockchainTxn;

@Configuration
public class GemfireConfiguration {
	
    @Value("${gemfire.locator.host:localhost}")
    private String locatorHost;

    @Value("${gemfire.locator.port:10334}")
    private Integer locatorPort;

    // Create a connection - client/server topology
    @Bean
    public ClientCache cache() {
        ClientCacheFactory ccf = new ClientCacheFactory();

        ccf.addPoolLocator(locatorHost, locatorPort);

        ccf.setPdxPersistent(false);
        ccf.setPdxReadSerialized(false);
        ccf.setPdxSerializer(new ReflectionBasedAutoSerializer("io.pivotal.dil.blockchain.entity.*"));

        return ccf.create();
    }

    // Get regions configured as non-caching proxies (data remains remote; a pure client-server topology)
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
