package songmicroservice;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		// TODO Auto-generated method stub
		if (songToAdd.getSongAlbum() == null || songToAdd.getSongArtistFullName() == null || songToAdd.getSongName() == null) {
			return new DbQueryStatus("Missing params error", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		this.db.insert(songToAdd);
		DbQueryStatus goodResponse = new DbQueryStatus("Song successfully added", DbQueryExecResult.QUERY_OK);
		goodResponse.setData(songToAdd);
		return goodResponse;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		// TODO Auto-generated method stub
		Query query = new Query();
		ObjectId newSongId = new ObjectId(songId); // converts to the ObjectId Mongo gives
		query.addCriteria(Criteria.where("_id").is(newSongId));
		List<Song> song = this.db.find(query, Song.class);
		if (song.isEmpty()) { // if no songs are found using this query
			return new DbQueryStatus("Song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		if (song.size() > 1) {
			return new DbQueryStatus("Query Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		Song songTitle = song.get(0); // should only return 1
		DbQueryStatus goodResponse = new DbQueryStatus("Song found", DbQueryExecResult.QUERY_OK);
		goodResponse.setData(songTitle);
		return goodResponse;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		// TODO Auto-generated method stub
		Query query = new Query();
		ObjectId newSongId = new ObjectId(songId); // converts to the ObjectId Mongo gives
		query.addCriteria(Criteria.where("_id").is(newSongId));
		List<Song> song = this.db.find(query, Song.class);
		if (song.isEmpty()) { // if no songs are found using this query
			return new DbQueryStatus("Song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		if (song.size() > 1) {
			return new DbQueryStatus("Query Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		String songTitle = song.get(0).getSongName(); // should only return 1
		DbQueryStatus goodResponse = new DbQueryStatus("Song title found", DbQueryExecResult.QUERY_OK);
		goodResponse.setData(songTitle);
		return goodResponse;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		// TODO Auto-generated method stub
		Query query = new Query();
		ObjectId newSongId = new ObjectId(songId);
		query.addCriteria(Criteria.where("_id").is(newSongId));
		List<Song> deletedSong = this.db.findAllAndRemove(query, Song.class);
		if (deletedSong.isEmpty()) { // if no songs are found using this query
			return new DbQueryStatus("Song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		if (deletedSong.size() > 1) {
			return new DbQueryStatus("Query Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		DbQueryStatus goodResponse = new DbQueryStatus("Song found and removed", DbQueryExecResult.QUERY_OK);
		
		return goodResponse;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		Query query = new Query();
		ObjectId newSongId = new ObjectId(songId); // converts to the ObjectId Mongo gives
		query.addCriteria(Criteria.where("_id").is(newSongId));
		List<Song> song = this.db.find(query, Song.class);
		if (song.isEmpty()) { // if no songs are found using this query
			return new DbQueryStatus("Song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		if (song.size() > 1) {
			return new DbQueryStatus("Query Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		long songFavs = song.get(0).getSongAmountFavourites();
		
		Update update = new Update();
		if (!shouldDecrement) {
			update.set("songAmountFavourites", songFavs + 1);
		} else if (shouldDecrement && songFavs > 0) {
			update.set("songAmountFavourites", songFavs - 1);
		} else if (shouldDecrement && songFavs == 0) {
			update.set("songAmountFavourites", songFavs);
		}
		this.db.updateFirst(query, update, Song.class);
		DbQueryStatus response = new DbQueryStatus("Song favourites updated", DbQueryExecResult.QUERY_OK);
		
		if (shouldDecrement && songFavs == 0) {
			response = new DbQueryStatus("Song favourites can't be negative", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		return response;
	}
}