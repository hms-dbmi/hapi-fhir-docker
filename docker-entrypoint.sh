#!/bin/bash -e

# Make everything executable
chmod a+x /docker-entrypoint-init.d/*

# Find all entrypoint scripts scripts
for f in /docker-entrypoint-init.d/*; do
    case "$f" in
        *.sh)  echo "Running $f"; . "$f" ;;
        *)     echo "Ignoring $f" ;;
    esac
    echo
done

# Run the command
exec "$@"