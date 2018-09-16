#!/bin/bash -e

# Check if JWT auth should be disabled
echo "dbmi.jwt_auth_enabled=${JWT_AUTH_ENABLED}" > /usr/local/tomcat/dbmi.application.properties

echo "JWT: Updating JWT auth enabled to: ${JWT_AUTH_ENABLED}"