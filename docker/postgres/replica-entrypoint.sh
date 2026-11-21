#!/bin/sh
set -e
if [ ! -s "$PGDATA/PG_VERSION" ]; then
  mkdir -p "$PGDATA" && chmod 700 "$PGDATA"
  pg_basebackup -h postgres -U replicator -D "$PGDATA" -R -X stream
fi
exec postgres
