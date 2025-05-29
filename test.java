# Stop and remove any existing container with the same name
if [ "$(docker ps -aq -f name=chroma-backend-container)" ]; then
    echo "Removing existing container..."
    docker rm -f chroma-backend-container
fi

echo "Running container on port 5000"
docker run -d -p 5000:5000 --name chroma-backend-container $FULL_IMAGE_NAME

echo "Container is running. Access the backend on http://localhost:5000"
