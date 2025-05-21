import sys
import pysqlite3
import subprocess

# Podmień sqlite3 → pysqlite3
sys.modules["sqlite3"] = pysqlite3

# Uruchom Chroma CLI z widocznym outputem
subprocess.run([
    sys.executable, "-m", "chromadb.cli", "run", "--path", "./chroma_data"
], stdout=sys.stdout, stderr=sys.stderr)
