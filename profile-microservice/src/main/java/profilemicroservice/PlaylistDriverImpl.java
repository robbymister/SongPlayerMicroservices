package profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.List;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
		DbQueryStatus queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult resultNoLike = trans.run("MATCH (:profile {userName: $user})-[s:created]->(pl)" + 
						" MATCH (:playlist {plName: pl.plName})-[r:includes]-(f:song {songId: $id})" + 
						" RETURN f", parameters("user", userName, "id", songId));
				// above query checks if the "includes" relationship does exist
				List<Record> resultList = resultNoLike.list();
				if (resultList.size() == 0) { // if "includes" DNE then CANNOT like
					trans.run("MATCH (:profile {userName: $user})-[r:created]->(pl)"
							+ " MATCH (s:song) WHERE s.songId = $id"
							+ " CREATE (pl)-[f:includes]->(s)", parameters("user", userName, "id", songId));
					queryStatus.setMessage("Song successfully liked");
					queryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				} else {
					queryStatus.setMessage("Song already liked");
					queryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				trans.success();
			}
			session.close();
		}
		return queryStatus;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		DbQueryStatus queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult resultNoLike = trans.run("MATCH (:profile {userName: $user})-[s:created]->(pl)" + 
						" MATCH (:playlist {plName: pl.plName})-[r:includes]-(f:song {songId: $id})" + 
						" RETURN f", parameters("user", userName, "id", songId));
				// above query checks if the "includes" relationship does exist
				List<Record> resultList = resultNoLike.list();
				if (resultList.size() > 0) { // if "includes" exists then CAN unlike
					trans.run("MATCH (:profile {userName: $user})-[s:created]->(pl)" + 
							" MATCH (:playlist {plName: pl.plName})-[r:includes]-(:song {songId: $id})" + 
							" DELETE r", parameters("user", userName, "id", songId));
					queryStatus.setMessage("Song successfully unliked");
					queryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				} else {
					queryStatus.setMessage("Song already unliked");
					queryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				
				trans.success();
			}
			session.close();
		}
		return queryStatus;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		DbQueryStatus retStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				StatementResult result = trans.run("MATCH (s:song) WHERE s.songId = $id RETURN s",
						parameters( "id", songId));
				// above query checks if a song with that id exists
				List<Record> resultList = result.list();
				if (resultList.size() == 1) {
					trans.run(" MATCH (s:song) WHERE s.songId = $id DETACH DELETE s",
							parameters( "id", songId));
					retStatus.setMessage("Song successfully deleted from database");
					retStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				} else {
					retStatus.setMessage("Song not found");
					retStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				trans.success();
			}
			session.close();
		}
		return retStatus;
	}
}
