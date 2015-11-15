package com.benweisel.clevelandwin.db.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DynamoDBConfigFactory
{
	@JsonProperty private String key;
	@JsonProperty private String secret;
	@JsonProperty private String tablePrefix;
	@JsonProperty private String region;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getTablePrefix() {
		return tablePrefix;
	}

	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}
	
	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public DynamoDBConfig build() {
		DynamoDBConfig config = new DynamoDBConfig();
		config.setKey(key);
		config.setSecret(secret);
		config.setTablePrefix(tablePrefix);
		config.setRegion(region);
        return config;
    }
}
