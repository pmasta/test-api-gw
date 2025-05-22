# Ustaw Flask jako ENTRYPOINT (foreground), a Chroma w tle
ENTRYPOINT ["bash", "-c", "\
  chroma run --path /app/chroma_data --port 9000 > /app/chroma.log 2>&1 & \
  python flask_app.py"]
