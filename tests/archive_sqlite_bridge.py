"""Host-only SQLite bridge. No dependency, code or database is shipped in the APK."""
import json
import sqlite3
import sys
sys.stdin.reconfigure(encoding="utf-8")
sys.stdout.reconfigure(encoding="utf-8")
connection = sqlite3.connect(sys.argv[1], isolation_level=None)
for line in sys.stdin:
    try:
        request = json.loads(line)
        cursor = connection.execute(request["sql"], request["args"])
        columns = [c[0] for c in cursor.description] if cursor.description else []
        reply = dict(columns=columns, rows=cursor.fetchall() if columns else [],
                     last=cursor.lastrowid, changes=cursor.rowcount)
    except Exception as error:
        reply = {"error": str(error)}
    print(json.dumps(reply, ensure_ascii=False), flush=True)
connection.close()
