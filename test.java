import pysqlite3
import sys
import os

# Podmiana sqlite3 → pysqlite3
sys.modules["sqlite3"] = pysqlite3

# Uruchom chroma jako osobny proces CLI
os.execvp("chroma", ["chroma", "run", "--path", "./chroma_data"])
