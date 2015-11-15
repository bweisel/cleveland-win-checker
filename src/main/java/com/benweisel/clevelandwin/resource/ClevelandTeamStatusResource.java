package com.benweisel.clevelandwin.resource;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.benweisel.clevelandwin.api.ClevelandTeams;
import com.benweisel.clevelandwin.api.TeamStatus;
import com.benweisel.clevelandwin.api.TeamStatusResponse;
import com.benweisel.clevelandwin.db.dao.ClevelandTeamStatusDAO;
import com.benweisel.clevelandwin.db.entity.TeamStatusEntity;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The resource the exposes the API to check the game status of each
 * Cleveland team (Browns, Cavs, Indians).
 * 
 * @author bweisel
 */
@Path("/") 
@Produces(MediaType.TEXT_HTML) // Arduino needs this to be the Content-Type
public class ClevelandTeamStatusResource
{
	private static final Logger log = LoggerFactory.getLogger(ClevelandTeamStatusResource.class);
	
	// ESPN Bottomline 'APIs'
	private static final String NFL_URL = "http://sports.espn.go.com/nfl/bottomline/scores";
	private static final String NBA_URL = "http://sports.espn.go.com/nba/bottomline/scores";
	private static final String MLB_URL = "http://sports.espn.go.com/mlb/bottomline/scores";
	
	private ObjectMapper mapper = null;
	private ClevelandTeamStatusDAO teamDAO;
	
	/**
	 * Build the resource
	 * 
	 * @param mapper the Jackson {@link ObjectMapper}
	 * @param teamDAO the {@link ClevelandTeamStatusDAO} to interact with DynamoDB
	 */
	public ClevelandTeamStatusResource(ObjectMapper mapper, ClevelandTeamStatusDAO teamDAO) {
		mapper = new ObjectMapper();
		this.teamDAO = teamDAO;
	}
	
	/**
	 * The main GET API
	 * 
	 * @return the {@link TeamStatusResponse} JSON object
	 * @throws WebApplicationException
	 */
	@GET
	@Timed
	@Path("teams/status")
	public TeamStatusResponse getTeamStatus() throws WebApplicationException {
		log.info("Entered /team/status API");
		
		TeamStatusEntity brownsDBStatus = null;
		TeamStatusEntity cavsDBStatus = null;
		TeamStatusEntity indiansDBStatus = null;
		try {
			brownsDBStatus = teamDAO.getTeamRecord(ClevelandTeams.BROWNS);
			cavsDBStatus = teamDAO.getTeamRecord(ClevelandTeams.CAVS);
			indiansDBStatus = teamDAO.getTeamRecord(ClevelandTeams.INDIANS);
		} catch (Exception e) {
			log.error("Error calling DynamoDB to get team records! " + e.getMessage(), e);
			throw new WebApplicationException("Could not get team status");
		}
		
		CloseableHttpClient httpClient = null;
		try {
			// Execute call to ESPN bottomline service to get real-time game updates
			// Update the status record in Dynamo accordingly
			httpClient = HttpClients.createDefault();
			TeamStatus brownsStatus = teamDAO.updateTeamStatus(getGameInfo(httpClient, NFL_URL), brownsDBStatus);
			TeamStatus cavsStatus = teamDAO.updateTeamStatus(getGameInfo(httpClient, NBA_URL), cavsDBStatus);
			TeamStatus tribeStatus = teamDAO.updateTeamStatus(getGameInfo(httpClient, MLB_URL), indiansDBStatus);

			// Return the results
			TeamStatusResponse response = new TeamStatusResponse(brownsStatus, cavsStatus, tribeStatus);
			log.info(mapper.writeValueAsString(response));
			return response;
		} catch (Exception e) {
			log.error("Error parsing score data from ESPN: " + e.getMessage(), e);
			throw new WebApplicationException("Could not get team status");
		} finally {
			try {
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				log.error("Error closing HTTP client in finally block: " + e.getMessage(), e);
			}
		}
	}
	

	/*
	 * Executes the HTTP call to get the game data. Not responsible for closing
	 * the passed in HttpClient.
	 */
	private Optional<String> getGameInfo(CloseableHttpClient client, String url) throws Exception {
		CloseableHttpResponse response = null;
		try {
			// Execute call to ESPN bottomline service to get real-time game
			// updates
			HttpGet get = new HttpGet(url);
			response = client.execute(get);

			// Parse the response
			// The response is formatted as a URL query string, need to prepend
			// on a fake host though
			String responseData = EntityUtils.toString(response.getEntity());
			responseData = "http://test.com?p=1" + responseData; // hacky
			responseData = responseData.replaceAll("\\^", "");

			// Loop over results and find the Cleveland line
			List<NameValuePair> results = URLEncodedUtils.parse(new URI(responseData), Charsets.UTF_8.name());
			for (NameValuePair item : results) {
				if (item.getValue() != null && item.getValue().contains("Cleveland")) {
					return Optional.of(item.getValue());
				}
			}
			return Optional.ofNullable(null);
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}
}
