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
	private String chartName; // e.g. "hourly_volume"
	// Map key: column name, map value: column value
	// List<Map<String, Object>> l = jdbcTemplate.queryForList(sql);
	private List<Map<String,Object>> rows;
	
	public BarChartData(String chartName, List<Map<String, Object>> rows) {
		this.chartName = chartName;
		this.rows = rows;
	}
	
	public String toJsonString() throws JsonProcessingException {
		return mapper.writeValueAsString(this);
	}

	public String getChartName() {
		return chartName;
	}

	public List<Map<String, Object>> getRows() {
		return rows;
	}

}
