package profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Value;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		DbQueryStatus queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult resultAlreadyProfile = trans.run("MATCH (p:profile {userName: $userName})" + 
						" RETURN p", parameters("userName", userName));
				List<Record> resultList = resultAlreadyProfile.list();
				if (resultList.size() == 0) { // no profile with identical userName (satisfies constraint)
					trans.run("MERGE (n:profile {userName: $user, fullName: $full,"
							+ " password: $pass})", parameters("user", userName, "full", fullName, "pass", password));
					queryStatus.setMessage("Profile successfully added");
					queryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				} else {
					queryStatus.setMessage("Profile with that UserName already exists");
					queryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				
				trans.success();
			}
			session.close();
		}
		return queryStatus;
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		DbQueryStatus queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult resultAlreadyFollowed = trans.run("MATCH (:profile {userName: $userName})-[r:follows]->"
						+ "(p:profile {userName: $frnd}) RETURN p", parameters("userName", userName, "frnd", frndUserName));
				List<Record> resultList = resultAlreadyFollowed.list();
				if (resultList.size() == 0) { // means that user1 IS NOT following user2
					trans.run("MATCH (n:profile), (b:profile) WHERE n.userName = $userName AND b.userName = $userFriend"
							+ " CREATE (n)-[r:follows]->(b)", parameters("userName", userName, "userFriend", frndUserName));
					queryStatus.setMessage("User successfully followed");
					queryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				} else {
					queryStatus.setMessage("That user is already followed");
					queryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				
				trans.success();
			}
			session.close();
		}
		
		return queryStatus;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		DbQueryStatus queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult resultAlreadyFollowed = trans.run("MATCH (:profile {userName: $userName})-[r:follows]->"
						+ "(p:profile {userName: $frnd}) RETURN p", parameters("userName", userName, "frnd", frndUserName));
				List<Record> resultList = resultAlreadyFollowed.list();
				if (resultList.size() > 0) { // means that user1 IS following user2
					trans.run("MATCH (:profile {userName: $userName})-[r:follows]-"
							+ "(:profile {userName: $userFriend})"
							+ " DELETE r", parameters("userName", userName, "userFriend", frndUserName));
					queryStatus.setMessage("User successfully unfollowed");
					queryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				} else {
					queryStatus.setMessage("That user is not followed");
					queryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				trans.success();
			}
			session.close();
		}
		
		return queryStatus;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		ArrayList<String> allFriends = new ArrayList<String>();
		ArrayList<String> allSongIds = new ArrayList<String>();
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult result = trans.run("MATCH (:profile {userName: $userName})-[r:follows]->(friends)" + 
						" RETURN friends.userName", parameters("userName", userName));
				// above query grabs all the friends then inserts into a list
				List<Record> resultList = result.list(); // converts result to list
				allFriends = new ArrayList<String>();
				allSongIds = new ArrayList<String>();
				for (int i=0; i < resultList.size(); i++) { // for each friend, retrieve their liked song
					allFriends.add(resultList.get(i).get("friends.userName").asString());
					StatementResult resultAllSongs = trans.run("MATCH (:profile {userName: $user})-[r:created]->(pl)" + 
							"MATCH (:playlist {plName: pl.plName})-[s:includes]->(songs)" + 
							"RETURN songs.songId", parameters("user", allFriends.get(i)));
					List<Record> resultAllSongsList = resultAllSongs.list();
					for (int x=0; x < resultAllSongsList.size(); x++) {
						String song = resultAllSongsList.get(x).get("songs.songId").asString();
						if (!allSongIds.contains(song)) {
							allSongIds.add(song);
						}
					}
				}
				
				trans.success();
			}
			session.close();
		}
		DbQueryStatus response = new DbQueryStatus("Successfully retrieved list of friends liked songs", DbQueryExecResult.QUERY_OK);
		response.setData(allSongIds);
		return response;
	}
}
