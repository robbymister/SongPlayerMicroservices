# Microservices
Microservice project that uses Song and Profile microservices to perform several functions. This is written in Java and uses Neo4j and MongoDB databases.

These features are split between the two microservices. Each profile only maintains a single playlist of their liked songs. 

 # Playlist Microservices (Neo4j & MongoDB, the songIDs match between DB):
 -Like Song: checks if a song is liked already (can't like twice), if the song is not liked then add it to the playlist
 
 -Unlike Song: checks if a song is not in playlist (can't unlike twice), if the song is liked then remove it from the playlist
 
 -Delete Song From DB: deletes song from DB if the songID is found
 
 
 # Profile Microservices (Neo4J):
 -Create Profile: creates a profile if the username chosen doesn not already exist
 
 -Follow Friend: checks if another profile is followed, if it not already followed then add it to profile's "follows" property
 
 -Unfollow Friend: checks if another profile is not followed, if it is followed then remove it from profile's "follows" property
 
 -Get All Friends Liked Songs: goes through a profile's "follows" property and retrieves all songs likes from each friends playlist
 
 
 # Song Microservices (MongoDB):
 -Add Song: checks DB if the song ID already exists, if not then add it to DB
 
 -Find Song By ID: retrieves JSON for a song if its ID exists
 
 -Get Song Title By ID: retrieves song title for a song if its ID exists
 
 -Delete Song By ID: deletes song from DB if its ID exists
 
 -Update Song Favourites Count: if a profile likes/unlikes a song, then add/remove a like from a songs like total
 
