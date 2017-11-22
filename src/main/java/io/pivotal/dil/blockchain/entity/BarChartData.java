package io.pivotal.dil.blockchain.entity;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

public class BarChartData {
	
	private static ObjectMapper mapper;
	static {
		mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
	}
	private String op; // e.g. "hourly_volume"
	// Map key: column name, map value: column value
	// List<Map<String, Object>> l = jdbcTemplate.queryForList(sql);
	private List<Map<String,Object>> rows;
	
	public BarChartData(String op, List<Map<String, Object>> rows) {
		this.op = op;
		this.rows = rows;
	}
	
	public String toJsonString() throws JsonProcessingException {
		return mapper.writeValueAsString(this);
	}

	public String getOp() {
		return op;
	}

	public List<Map<String, Object>> getRows() {
		return rows;
	}

}
