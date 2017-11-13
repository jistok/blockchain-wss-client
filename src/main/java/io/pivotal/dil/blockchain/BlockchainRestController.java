package io.pivotal.dil.blockchain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.geode.cache.Region;

import io.pivotal.dil.blockchain.entity.BlockchainItem;
import io.pivotal.dil.blockchain.entity.BlockchainTxn;

@RestController
public class BlockchainRestController {
	
	@Autowired
	private Region<String /* hash */, BlockchainTxn> blockchainTxnRegion;

	@Autowired
	private Region<String /* BlockchainItem.genId() */, BlockchainItem> blockchainItemRegion;

	// Get the JSON version of a BlockchainTxn for the given hash value
    @RequestMapping(method = RequestMethod.GET, value = "/getTxn/{hash}")
    public String getBlockchainTxnFromHash(@PathVariable String hash) {
    		String rv = "{}";
    		BlockchainTxn txn = blockchainTxnRegion.get(hash);
    		if (txn != null) {
    			try {
					rv = txn.toJSON();
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
    		}
    		return rv;
    }

}
