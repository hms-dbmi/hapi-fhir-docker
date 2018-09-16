#!/bin/bash

# Read defaults and export them, leaving existing values intact
while read -r var value; do

    # Check if defined already
    if [ -z "${!var}" ]; then
        echo "ENV: $var not defined, will default to: $value"
    fi

  eval `echo "export $var=\"${!var:-$value}\""`
done < /docker-entrypoint-resources.d/defaults.env