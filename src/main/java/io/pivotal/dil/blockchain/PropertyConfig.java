package io.pivotal.dil.blockchain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PropertyConfig {

	@Value("${locators:localhost[10334]}")
	public String locators;
	
	@Value("${username:gfadmin}")
	public String username;
	
	@Value("${password:}")
	public String password;

}
