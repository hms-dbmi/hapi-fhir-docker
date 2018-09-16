#!/bin/bash -e

# Make everything executable
chmod a+x /docker-entrypoint-init.d/*
chmod a+x /docker-entrypoint.d/*

# Find all entrypoint initialization scripts scripts
for f in /docker-entrypoint-init.d/*; do
    case "$f" in
        *.sh)  echo -e "Running init: $f"; seq -s= 80|tr -d '[:digit:]'; echo ""; . "$f" ;;
        *)     echo "Ignoring $f" ;;
    esac
    echo
done

# Find all entrypoint service scripts
for f in /docker-entrypoint.d/*; do
    case "$f" in
        *.sh)  echo -e "Running: $f"; seq -s= 80|tr -d '[:digit:]'; echo ""; . "$f" ;;
        *)     echo "Ignoring $f" ;;
    esac
    echo
done

# Run the command
exec "$@"