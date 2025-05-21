import pysqlite3
import sys
import subprocess

sys.modules["sqlite3"] = pysqlite3

subprocess.run([
    sys.executable, "-m", "chromadb.cli", "run", "--path", "./chroma_data"
])
