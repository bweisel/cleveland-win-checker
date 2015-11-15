package com.benweisel.clevelandwin.db.dao;

import java.util.Optional;
import java.util.regex.Pattern;

import com.benweisel.clevelandwin.api.ClevelandTeams;
import com.benweisel.clevelandwin.api.TeamStatus;
import com.benweisel.clevelandwin.db.entity.TeamStatusEntity;

/**
 * Most of the application logic is in here. It handles interacting with Dynamo
 * and parsing the ESPN Bottomline API results to determine the status of each
 * team.
 * 
 * @author bweisel
 */
public interface ClevelandTeamStatusDAO
{
	// Regex patterns
	public static final Pattern teamPattern = Pattern.compile("\\b[a-zA-Z]+ \\b\\d+");
	public static final Pattern clevelandPattern = Pattern.compile("\\bCleveland \\b\\d+");
	public static final Pattern scorePattern = Pattern.compile("\\d+");
	public static final Pattern inProgressPattern = Pattern.compile("(\\d+:\\d+ IN \\d)");
	public static final Pattern upcomingPattern = Pattern.compile("(\\d+:\\d+ AM|PM)");
	
	/**
	 * Gets the team status record in Dynamo
	 * 
	 * @param teamName the {@link ClevelandTeams} team name
	 * @return the {@link TeamStatusEntity} from Dynamo 
	 */
	TeamStatusEntity getTeamRecord(ClevelandTeams team);
	
	/**
	 * Updates the team record according to the score line passed in. Most of the logic
	 * is done here, although it really shouldn't be.
	 * 
	 * @param scoreLine the result of calling the ESPN Bottomline API
	 * @param teamStatusEntity the {@link TeamStatusEntity} representing the current status of the team 
	 * @return the {@link TeamStatus} response to send back to the client
	 */
	TeamStatus updateTeamStatus(Optional<String> scoreLine, TeamStatusEntity teamStatusEntity);
	
}
