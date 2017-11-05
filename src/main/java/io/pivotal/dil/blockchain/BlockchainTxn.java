package io.pivotal.dil.blockchain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

public class BlockchainTxn {

	private long lockTime;
	private int ver;
	private int size;
	private long time; // Date date = new Date(time * 1000)
	private long txIndex;
	private int vinSz; // Size of inputs (see above)
	private String hash; // Probably, the primary key
	private int voutSz; // Size of out (see below)
	private String relayedBy; // An IP address: "0.0.0.0" or similar
	private List<BlockchainInput> inputs;
	private List<BlockchainItem> out;
	// Derived from time field
	private Date timeAsDate;

	// For the trip back to JSON
	private static final ObjectMapper mapper;
	static {
		mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
	}

	public BlockchainTxn() {
		super();
	}
	
	public String toJSON() throws JsonProcessingException {
		return mapper.writeValueAsString(this);
	}

	public Date getTimeAsDate() {
		return timeAsDate;
	}

	@JsonGetter("timeAsDate")
	public String getTimeAsString() {
		return timeAsDate.toString();
	}

	/*
	 * TODO: Get an instance from a JSON document
	 */
	public static BlockchainTxn fromJSON(String jsonString) {
		BlockchainTxn rv = null;
		try {
			JSONObject x = new JSONObject(jsonString).getJSONObject("x");
			rv = new BlockchainTxn();
			rv.lockTime = x.getLong("lock_time");
			rv.ver = x.getInt("ver");
			rv.size = x.getInt("size");
			rv.time = x.getLong("time");
			rv.timeAsDate = new Date(rv.time * 1000);
			rv.txIndex = x.getLong("tx_index");
			rv.vinSz = x.getInt("vin_sz");
			rv.hash = x.getString("hash");
			rv.voutSz = x.getInt("vout_sz");
			rv.relayedBy = x.getString("relayed_by");
			// Add the inputs
			rv.inputs = new ArrayList<>(rv.vinSz);
			JSONArray inputArray = x.getJSONArray("inputs");
			for (int i = 0; i < inputArray.length(); i++) {
				rv.inputs.add(BlockchainInput.fromJSONObject(inputArray.getJSONObject(i)));
			}
			// Add the outputs
			rv.out = new ArrayList<>(rv.voutSz);
			JSONArray outArray = x.getJSONArray("out");
			for (int i = 0; i < outArray.length(); i++) {
				rv.out.add(BlockchainItem.fromJSONObject(outArray.getJSONObject(i)));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return rv;
	}

	public long getLockTime() {
		return lockTime;
	}

	public void setLockTime(long lockTime) {
		this.lockTime = lockTime;
	}

	public int getVer() {
		return ver;
	}

	public void setVer(int ver) {
		this.ver = ver;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public List<BlockchainInput> getInputs() {
		return inputs;
	}

	public void setInputs(List<BlockchainInput> inputs) {
		this.inputs = inputs;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getTxIndex() {
		return txIndex;
	}

	public void setTxIndex(long txIndex) {
		this.txIndex = txIndex;
	}

	public int getVinSz() {
		return vinSz;
	}

	public void setVinSz(int vinSz) {
		this.vinSz = vinSz;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public int getVoutSz() {
		return voutSz;
	}

	public void setVoutSz(int voutSz) {
		this.voutSz = voutSz;
	}

	public String getRelayedBy() {
		return relayedBy;
	}

	public void setRelayedBy(String relayedBy) {
		this.relayedBy = relayedBy;
	}

	public List<BlockchainItem> getOut() {
		return out;
	}

	public void setOut(List<BlockchainItem> out) {
		this.out = out;
	}

}
