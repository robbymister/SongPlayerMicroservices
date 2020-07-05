package profilemicroservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import profilemicroservice.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));

		DbQueryStatus queryStatus = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
		
		String username = params.get("userName");
		String fullname = params.get("fullName");
		String password = params.get("password");
		if (password != null && fullname != null && username != null) {
			queryStatus = this.profileDriver.createUserProfile(username, fullname, password);
		} else {
			queryStatus = new DbQueryStatus("Missing params for profile creation", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		response.put("message", queryStatus.getMessage());
		response = Utils.setResponseStatus(response, queryStatus.getdbQueryExecResult(), null);
		
		return response;
	}

	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus queryStatus = this.profileDriver.followFriend(userName, friendUserName);
		
		response.put("message", queryStatus.getMessage());
		response = Utils.setResponseStatus(response, queryStatus.getdbQueryExecResult(), null);
		
		return response;
	}

	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		ObjectMapper mapper = new ObjectMapper();
		DbQueryStatus queryStatus = this.profileDriver.getAllSongFriendsLike(userName);
		
		try {
			if ((userName) != null) {
				ArrayList<String> allSongIds = (ArrayList<String>) queryStatus.getData();
				ArrayList<Object> allSongData = new ArrayList<Object>();
				for (int i=0; i < allSongIds.size(); i++) {
					String songId = allSongIds.get(i);
					String path = String.format("http://localhost:3001/getSongById/%s", 
							songId);
					
					HttpUrl.Builder urlBuilder = HttpUrl.parse(path).newBuilder();
					String url = urlBuilder.build().toString();

					Request requestSongService = new Request.Builder()
							.url(url)
							.method("GET", null)
							.build();
					Call call = client.newCall(requestSongService);
					Response responseFromFavCountMs = null;
					String songIdBody = "{}";
					
					try {
						responseFromFavCountMs = call.execute();

						// check for response successful, if status is not 200 - 300
						if(!responseFromFavCountMs.isSuccessful()){
							throw new IOException("INVALID SongId" + responseFromFavCountMs);
						}

						// status code: 400, 405, etc
						songIdBody = responseFromFavCountMs.body().string();
						allSongData.add(mapper.readValue(songIdBody, Map.class)); // should finalQueryStatus here
						queryStatus.setData(allSongData);
					} catch (Exception e) {
						response.put("message", "There was an error liking the song");
						response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}
			} else {
				response.put("message", "Params usage error");
				response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch(Exception e) {
			e.printStackTrace();
			response.put("message", "There was an error finding your friend");
			response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		response.put("message", "Friends liked songs successfully found.");
		response = Utils.setResponseStatus(response, queryStatus.getdbQueryExecResult(), queryStatus.getData());
		
		return response;
	}


	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		DbQueryStatus queryStatus = this.profileDriver.unfollowFriend(userName, friendUserName);
		
		response.put("message", queryStatus.getMessage());
		response = Utils.setResponseStatus(response, queryStatus.getdbQueryExecResult(), null);
		
		return response;
	}

	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		String path = String.format("http://localhost:3001/updateSongFavouritesCount/%s?shouldDecrement=false", 
				songId);
		
		try {
			if ((userName) != null && (songId) != null) {
				HttpUrl.Builder urlBuilder = HttpUrl.parse(path).newBuilder();
				String url = urlBuilder.build().toString();
				
				RequestBody body = RequestBody.create(null, new byte[0]);

				Request requestSongService = new Request.Builder()
						.url(url)
						.method("PUT", body)
						.build();
				Call call = client.newCall(requestSongService);
				Response responseFromFavCountMs = null;
				String favCountBody = "{}";
				
				try {
					responseFromFavCountMs = call.execute();

					// check for response successful
					if(!responseFromFavCountMs.isSuccessful()){
						throw new IOException("INVALID SongId" + responseFromFavCountMs);
					}

					favCountBody = responseFromFavCountMs.body().string();
				} catch (Exception e) {
					response.put("message", "There was an error liking the song");
					response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
				}
				DbQueryStatus queryStatus = this.playlistDriver.likeSong(userName, songId);
				
				response.put("message", queryStatus.getMessage());
				response = Utils.setResponseStatus(response, queryStatus.getdbQueryExecResult(), null);
			}
			else {
				response.put("message", "Params usage error");
				response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch(Exception e) {
			response.put("message", "There was an error liking the song");
			response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return response;
	}

	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		String path = String.format("http://localhost:3001/updateSongFavouritesCount/%s?shouldDecrement=true", 
				songId);
		
		try {
			if ((userName) != null && (songId) != null) {
				HttpUrl.Builder urlBuilder = HttpUrl.parse(path).newBuilder();
				String url = urlBuilder.build().toString();
				
				RequestBody body = RequestBody.create(null, new byte[0]);

				Request requestSongService = new Request.Builder()
						.url(url)
						.method("PUT", body)
						.build();
				Call call = client.newCall(requestSongService);
				Response responseFromFavCountMs = null;
				String favCountBody = "{}";
			
				try {
					responseFromFavCountMs = call.execute();

					// check for response successful
					if(!responseFromFavCountMs.isSuccessful()){
						throw new IOException("INVALID SongId" + responseFromFavCountMs);
					}

					favCountBody = responseFromFavCountMs.body().string();
				} catch (Exception e) {
					response.put("message", "There was an error unliking the song");
					response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
				}
				DbQueryStatus queryStatus = this.playlistDriver.unlikeSong(userName, songId);
				
				response.put("message", queryStatus.getMessage());
				response = Utils.setResponseStatus(response, queryStatus.getdbQueryExecResult(), null);
			}
			else {
				response.put("message", "Params usage error");
				response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch(Exception e) {
			response.put("message", "There was an error unliking the song");
			response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return response;
	}

	@RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		DbQueryStatus queryStatus = this.playlistDriver.deleteSongFromDb(songId);

		response.put("message", queryStatus.getMessage());
		response = Utils.setResponseStatus(response, queryStatus.getdbQueryExecResult(), null);
		return response;
	}
}