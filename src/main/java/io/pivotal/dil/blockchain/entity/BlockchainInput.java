package io.pivotal.dil.blockchain.entity;

import org.json.JSONException;
import org.json.JSONObject;

public class BlockchainInput {
	
	private long sequence;
	private String script;
	private BlockchainItem prevOut;

	public BlockchainInput() {
		super();
	}

	public long getSequence() {
		return sequence;
	}

	public void setSequence(long sequence) {
		this.sequence = sequence;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public BlockchainItem getPrevOut() {
		return prevOut;
	}

	public void setPrevOut(BlockchainItem prevOut) {
		this.prevOut = prevOut;
	}

	/*
	 {
      "sequence" : 4294967295,
      "prev_out" : {
        "spent" : true,
        "tx_index" : 297927628,
        "type" : 0,
        "addr" : "3FnVYDyPt2NPWN8ttpEbPeTiYcMNcVZKoZ",
        "value" : 127870000,
        "n" : 2,
        "script" : "a9149a9ae48d0d64b1fdae3a32e72da59b65cc31c6ed87"
      },
      "script" : "220020962d040581c14c1d1f060056b28ec0d2f5946c738fbe817d8d2f8fe3769bba0a"
    }
	 */
	public static BlockchainInput fromJSONObject(JSONObject jsonObject) {
		BlockchainInput rv = null;
		try {
			rv = new BlockchainInput();
			rv.sequence = jsonObject.getLong("sequence");
			rv.script = jsonObject.getString("script");
			rv.prevOut = BlockchainItem.fromJSONObject(jsonObject.getJSONObject("prev_out"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return rv;
	}

}
