#!/bin/bash

# Use this script to update the server with the latest from the develop branch.
# Your current directory must be the scripts folder when you run the script.

if [ "$#" -ne 1 ]; then
  echo "Error: Server username and address required"
  echo "Example: ./update-api.sh ubuntu@server.com"
  exit 1
fi

SERVER_ADDRESS="$1"

echo "Changing directory to project root"
cd ..

echo "Pulling latest code from develop branch"
git checkout develop
git pull

./mvnw clean -Dmaven.test.skip=true package
if [ $? -ne 0 ]; then
  echo "Error running 'mvnw'. Aborting update."
  exit 1
fi

echo "Changing directory to target"
cd target

echo "Renaming wise.war to ROOT.war"
mv wise.war ROOT.war

echo "Copying ROOT.war to server"
scp ROOT.war $SERVER_ADDRESS:./

echo "Running deploy-api.sh script on server"
ssh $SERVER_ADDRESS "sudo ~/scripts/deploy-api.sh"

