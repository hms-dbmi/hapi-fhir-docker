#!/bin/bash -e

# Check if nginx is running, reload it or start it
if [ -e $DBMI_NGINX_PID_PATH ]; then
    nginx -s reload
else
    nginx
fi