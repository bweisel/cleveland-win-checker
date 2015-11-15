package com.benweisel.clevelandwin;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.benweisel.clevelandwin.db.config.DynamoDBConfig;
import com.benweisel.clevelandwin.db.dao.ClevelandTeamStatusDAO;
import com.benweisel.clevelandwin.db.dao.ClevelandTeamStatusDAOImpl;
import com.benweisel.clevelandwin.healthcheck.AppHealthCheck;
import com.benweisel.clevelandwin.resource.ClevelandTeamStatusResource;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ClevelandWinCheckerApp extends Application<ClevelandWinCheckerConfig>
{
	// Main
	public static void main(String[] args) throws Exception {
		new ClevelandWinCheckerApp().run(args);
	}

    @Override
    public String getName() {
        return "Cleveland Sports Real-time Win Checker API";
    }

	@Override
	public void initialize(Bootstrap<ClevelandWinCheckerConfig> bootstrap) {
		 // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
	}

	@Override
	public void run(ClevelandWinCheckerConfig appConfig, Environment environment) throws Exception {
		// Create ObjectMapper
		final ObjectMapper mapper = new ObjectMapper()
							.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		
		// Create Dynamo DAO (User Teams)
		DynamoDBConfig dynamoConfig = appConfig.getDynamoDBConfigFactory().build();
		AmazonDynamoDBClient dynamoClient = new AmazonDynamoDBClient(
				new BasicAWSCredentials(dynamoConfig.getKey(), 
										dynamoConfig.getSecret()));
		dynamoClient.setRegion(Region.getRegion(Regions.fromName(dynamoConfig.getRegion())));
		DynamoDBMapper dbMapper = new DynamoDBMapper(dynamoClient);
		
		ClevelandTeamStatusDAO teamDAO = new ClevelandTeamStatusDAOImpl(dbMapper);
		
		// Create and register resources
		final ClevelandTeamStatusResource apiResource = new ClevelandTeamStatusResource(mapper, teamDAO);
		environment.jersey().register(apiResource);
		
		// Create and register healthchecks
		final AppHealthCheck appHealthCheck = new AppHealthCheck();
		environment.healthChecks().register("app", appHealthCheck);
	}

}
