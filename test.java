#!/bin/bash
set -e

IMAGE_NAME=chroma-backend
VERSION=1.0.0
FULL_IMAGE_NAME=nexus.example.com/your-group/${IMAGE_NAME}:${VERSION}
CONTAINER_NAME=chroma-backend-container

# If --pull is passed, skip build and pull from Nexus
if [[ "$1" == "--pull" ]]; then
  echo "Pulling Docker image from Nexus: $FULL_IMAGE_NAME"
  docker pull $FULL_IMAGE_NAME
else
  echo "Building Docker image locally: $FULL_IMAGE_NAME"
  docker build -t $FULL_IMAGE_NAME ./chroma
fi

# Stop and remove existing container if running
if [ "$(docker ps -aq -f name=$CONTAINER_NAME)" ]; then
    echo "Removing existing container: $CONTAINER_NAME"
    docker rm -f $CONTAINER_NAME
fi

echo "Running container on port 5000"
docker run -d -p 5000:5000 --name $CONTAINER_NAME $FULL_IMAGE_NAME

echo "Backend is running at http://localhost:5000"
