#!/bin/bash
#
# Seeds the local wiki database with demo content. Run it after the schema
# exists - the application creates it via Flyway on first start:
#
#   docker compose up -d && ./gradlew bootRunLocal   # once, then stop it
#   ./gradlew initializeDatabase
#
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5433}"
DB_NAME="${DB_NAME:-wiki}"
DB_USER="${DB_USER:-postgres}"
export PGPASSWORD="${PGPASSWORD:-db200}"

echo "Seeding $DB_NAME at $DB_HOST:$DB_PORT ..."

if ! command -v psql &>/dev/null; then
    echo "Error: 'psql' command not found. Install the PostgreSQL client first." >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$SCRIPT_DIR/local_database"

if [ ! -d "$SCRIPTS_DIR" ]; then
    echo "Directory $SCRIPTS_DIR doesn't exist or is not a directory." >&2
    exit 1
fi

while read -r file; do
    echo "Executing script: $file"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -q -f "$file"
done < <(find "$SCRIPTS_DIR" -type f -name "*.sql" | sort)

echo "Database seeding complete!"
