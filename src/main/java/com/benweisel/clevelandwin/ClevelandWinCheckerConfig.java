package com.benweisel.clevelandwin;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.benweisel.clevelandwin.db.config.DynamoDBConfigFactory;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class ClevelandWinCheckerConfig extends Configuration 
{	
	
	@Valid
	@NotNull
	private DynamoDBConfigFactory dynamoDB = new DynamoDBConfigFactory();
	
	@JsonProperty("dynamoDB")
	public DynamoDBConfigFactory getDynamoDBConfigFactory() {
		return dynamoDB;
	}

	@JsonProperty("dynamoDB")
	public void setDynamoDBConfigFactory(DynamoDBConfigFactory dynamoDB) {
		this.dynamoDB = dynamoDB;
	}
	
}
