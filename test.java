CMD ["-c", "\
  chroma run --path /app/chroma_data --port 9000 > /app/chroma.log 2>&1 & \
  python flask_app.py > /app/flask.log 2>&1 & \
  exec /bin/bash"]
