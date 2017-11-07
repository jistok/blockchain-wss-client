package io.pivotal.dil.blockchain.entity;

import org.json.JSONException;
import org.json.JSONObject;

/*
    {
        "spent" : true,
        "tx_index" : 297927628,
        "type" : 0,
        "addr" : "3FnVYDyPt2NPWN8ttpEbPeTiYcMNcVZKoZ",
        "value" : 127870000,
        "n" : 2,
        "script" : "a9149a9ae48d0d64b1fdae3a32e72da59b65cc31c6ed87"
     }
 */
public class BlockchainItem {

	private boolean spent;
	private long txIndex;
	private int type;
	private String addr; // Possible PK? Maybe, combined with txIndex?
	private long value;
	private int n;
	private String script;

	// The ID used to put/get these
	public String getId() {
		return genId(addr, txIndex);
	}

	// For use in looking one of these up by ID
	public static String genId(String addr, long txIndex) {
		return addr + "-" + txIndex; 
	}
	
	public BlockchainItem() {
	}

	public boolean isSpent() {
		return spent;
	}

	public void setSpent(boolean spent) {
		this.spent = spent;
	}

	public long getTxIndex() {
		return txIndex;
	}

	public void setTxIndex(long txIndex) {
		this.txIndex = txIndex;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	/*
	 * { "spent" : true, "tx_index" : 297927628, "type" : 0, "addr" :
	 * "3FnVYDyPt2NPWN8ttpEbPeTiYcMNcVZKoZ", "value" : 127870000, "n" : 2, "script"
	 * : "a9149a9ae48d0d64b1fdae3a32e72da59b65cc31c6ed87" }
	 */
	public static BlockchainItem fromJSONObject(JSONObject jsonObject) {
		BlockchainItem rv = null;
		try {
			rv = new BlockchainItem();
			rv.spent = jsonObject.getBoolean("spent");
			rv.txIndex = jsonObject.getLong("tx_index");
			rv.type = jsonObject.getInt("type");
			rv.addr = jsonObject.isNull("addr") ? "null" : jsonObject.getString("addr");
			rv.value = jsonObject.getLong("value");
			rv.n = jsonObject.getInt("n");
			rv.script = jsonObject.getString("script");
		} catch (JSONException e) {
			e.printStackTrace();
			System.out.println("JSON: " + jsonObject.toString());
		}
		return rv;
	}

}
