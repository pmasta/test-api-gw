import os
import subprocess
import sys

# Zmienna środowiskowa – wskazuje Pythona z podmienionym sqlite3
python_code = """
import pysqlite3
import sys
sys.modules['sqlite3'] = pysqlite3
import runpy
runpy.run_module('chromadb.cli', run_name='__main__')
"""

# Uruchom chroma z podmienionym sqlite3
subprocess.run(
    [sys.executable, "-c", python_code, "run", "--path", "./chroma_data"]
)
