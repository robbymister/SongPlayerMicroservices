#!/bin/bash

echo "----------------------------------------------------------------------------------------------------"
echo "****************************************************************************************************"
echo "----------------------------------------------------------------------------------------------------"

echo "-----BEGIN IMPORTING TEST DATA-----"

[ -d "song_db_backup" ] || mkdir "song_db_backup"

echo "-----Backing up existing  DB-----"
mongodump -d -test -o ./"song_db_backup"/`date +%Y-%m-%d_%H-%M-%S` || (echo "[ERROR] Could not create DB backup" && exit 1)
echo "-----Deleting existing  DB-----" 
mongo -test --eval "db.dropDatabase()" || (echo "[ERROR] Could not delete  DB" && exit 1)
echo "-----Importing test data from ./MOCK_DATA to  DB-----"
mongoimport --db -test --jsonArray --collection songs --file "./MOCK_DATA.json" || (echo "[ERROR] Could not import test data" && exit 1)

echo "-----END IMPORTING TEST DATA-----"