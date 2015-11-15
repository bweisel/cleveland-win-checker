package com.benweisel.clevelandwin.db.dao;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.benweisel.clevelandwin.api.ClevelandTeams;
import com.benweisel.clevelandwin.api.TeamStatus;
import com.benweisel.clevelandwin.db.entity.TeamStatusEntity;


public class ClevelandTeamStatusDAOImpl implements ClevelandTeamStatusDAO
{
	private static final Logger log = LoggerFactory.getLogger(ClevelandTeamStatusDAOImpl.class);
	
	private DynamoDBMapper db = null;
	
	public ClevelandTeamStatusDAOImpl(DynamoDBMapper db) {
		this.db = db;
	}
	
	@Override
	public TeamStatusEntity getTeamRecord(ClevelandTeams team) {
		return db.load(TeamStatusEntity.class, team.name().toLowerCase());
	}

	
	@Override
	public TeamStatus updateTeamStatus(Optional<String> scoreLine, TeamStatusEntity teamStatusEntity) {
		final String teamName = teamStatusEntity.getTeamName().toUpperCase();
		
		// Check if there was no Cleveland team in the bottomline result
		if (!scoreLine.isPresent()) {
			log.info("No game status result found for " + teamName);
			return TeamStatus.NO_OP;
		}
		
		// If the last win date is within the last 10 minutes return WIN!
		if (LocalDateTime.now()
						 .minusMinutes(10)
						 .isBefore(teamStatusEntity.getLastVictory())) {
			log.info(teamName + " won in the last 10 minutes!");
			return TeamStatus.WIN;
		}

		// Begin to parse the result from the bottomline call
		String fullScoreLine = scoreLine.get();
		log.info(fullScoreLine);
		
		int split = fullScoreLine.indexOf("(");
		String teamScores = fullScoreLine.substring(0, split);
		String timeLeft = fullScoreLine.substring(split);

		// Check if the game is in progress
		// If so, update the in game timestamp
		Matcher progressMatcher = inProgressPattern.matcher(timeLeft);
		if(progressMatcher.find()) {
			log.info(teamName + " game in progress.");
			teamStatusEntity.setInGame(true);
			teamStatusEntity.setInGameLastUpdated(LocalDateTime.now());
			db.save(teamStatusEntity);
			return TeamStatus.IN_PROGRESS;
		}
		
		// Check if the game is upcoming
		// If so, just return NO OP
		Matcher upcomingMatcher = upcomingPattern.matcher(timeLeft);
		if (upcomingMatcher.find()) {
			log.info(teamName + " game upcoming.");
			return TeamStatus.NO_OP;
		}

		// Game must be over, let's check the score!
		int cleScore = -1;
		int oppScore = -1;

		// This regex will essentially divide the teamScores line into 2 pieces:
		// Opponent city & score and Cleveland & score
		Matcher teamMatcher = teamPattern.matcher(teamScores);
		while (teamMatcher.find()) {
			// Parse the score int from the String
			String teamScore = teamMatcher.group();
			Matcher scoreMatcher = scorePattern.matcher(teamScore);
			scoreMatcher.find();
			int parsedScore = Integer.valueOf(scoreMatcher.group());

			// Determine if it's the Cleveland score or opponent score
			if (clevelandPattern.matcher(teamScore).matches()) {
				cleScore = parsedScore;
			} else {
				oppScore = parsedScore;
			}
		}
		
		// Make sure neither score is still -1 (indicating some error parsing)
		log.info("CLE: " + cleScore + " OPP: " + oppScore);
		if (cleScore < 0 || oppScore < 0) {
			log.info("Error parsing score values to determine winner! One value is below zero");
			return TeamStatus.NO_OP;
		}
		
		// If Cleveland won, and the in game timestamp is after the last victory, update the DB!
		// Otherwise, just update DB to set in game status to false.
		if (cleScore > oppScore && 
			teamStatusEntity.getInGameLastUpdated()
							.isAfter(teamStatusEntity.getLastVictory())) {
			
			teamStatusEntity.setLastVictory(LocalDateTime.now());
			teamStatusEntity.setInGame(false);
			db.save(teamStatusEntity);
			log.info(teamName + " JUST won!!!");
			return TeamStatus.WIN;
		} else {
			teamStatusEntity.setInGame(false);
			db.save(teamStatusEntity);
			log.info(teamName + " game over. Loss, or win is past the alert time.");
		}
		return TeamStatus.NO_OP;
	}

}
