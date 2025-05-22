RUN pip install --break-system-packages -r requirements.txt

# Problematic package – retry logic
RUN pip install --break-system-packages sentence-transformers || \
    (echo "Retrying..."; sleep 3; pip install --break-system-packages sentence-transformers)
