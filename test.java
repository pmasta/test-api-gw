import os
import sys
import subprocess

PYTHON_CODE = """
import pysqlite3
import sys
sys.modules['sqlite3'] = pysqlite3
import chromadb.cli
"""

# Uruchom chroma run z podmienionym sqlite3 i pełnym widocznym outputem
subprocess.run(
    [sys.executable, "-c", PYTHON_CODE, "run", "--path", "./chroma_data"],
    stdout=sys.stdout,
    stderr=sys.stderr
)
