package io.pivotal.dil.blockchain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.geode.cache.Region;

import io.pivotal.dil.blockchain.entity.BlockchainItem;
import io.pivotal.dil.blockchain.entity.BlockchainTxn;

@RestController
public class BlockchainRestController {

	@Autowired
	private Region<String /* hash */, BlockchainTxn> blockchainTxnRegion;

	@Autowired
	private Region<String /* BlockchainItem.genId() */, BlockchainItem> blockchainItemRegion;

	@Autowired
	private Region<String, String> gpdbResultRegion;

	@Autowired
	JdbcTemplate jdbcTemplate;

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

	// Get the CSV to drive the d3.js bar graph of Bitcoin volume as a function of hour of the day
	@RequestMapping(method = RequestMethod.GET, value = "/hourly-volume.csv")
	public String getHourlyVolumeForChart() {
		String sql = "SELECT\n" +
				"LPAD(DATE_PART('hour', t.time)::TEXT, 2, '0') || ':00' hour_of_day,\n" +
				"(((SUM(i.value / 100000000.0))/1000.0)::BIGINT) sum\n" +
				"FROM blockchain_txn t, blockchain_item i\n" +
				"WHERE t.hash = i.hash\n" +
				"GROUP BY hour_of_day\n" +
				"ORDER BY hour_of_day ASC;";
		// key is the hash of the SQL query string + "-" + the date to the hour
		Date now = new Date();
		DateFormat dateToTheHour = new SimpleDateFormat("yyyyMMddHH");
		String key = DigestUtils.sha256Hex(sql) + "-" + dateToTheHour.format(now);
		String rv = gpdbResultRegion.get(key);
		if (null == rv) {
			// Run query against GPDB
			// Ref. https://www.mkyong.com/spring/spring-jdbctemplate-querying-examples
			List<String> result = new ArrayList<String>();
			for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
				result.add((String) row.get("hour_of_day") + "," + row.get("sum"));
			}
			// Format the result as CSV, with the header
			rv = "hour,value"; // Header
			rv += "\n" + String.join("\n", result.toArray(new String[result.size()]));
			// Store this back into Gemfire
			gpdbResultRegion.put(key, rv);
		}
		return rv;
	}

}
