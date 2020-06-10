#!/bin/bash -e

# Ensure we have database properties
:   ${HAPI_DATASOURCE_DRIVER?: must be defined}
:   ${HAPI_DATASOURCE_URL?: must be defined}
:   ${HAPI_DATASOURCE_USERNAME?: must be defined}
:   ${HAPI_DATASOURCE_PASSWORD?: must be defined}
:   ${HAPI_SERVER_ADDRESS?: must be defined}

# Check static file envs
if [ ! -z $JWT_AUTH_ENABLED ]; then

:   ${JWT_ISSUER?: is required if JWT authn and authz are enabled}
:   ${JWT_AUDIENCE?: is required if JWT authn and authz are enabled}
:   ${JWT_COOKIE_NAME?: is required if JWT authn and authz are enabled}
:   ${JWT_HEADER_PREFIX?: is required if JWT authn and authz are enabled}
:   ${JWT_AUTHZ_CLAIM?: is required if JWT authn and authz are enabled}
:   ${JWT_ADMIN_GROUP?: is required if JWT authn and authz are enabled}

fi

echo "Envs check passed!"

# Check for debug mode
if [ ! -z $DBMI_DEBUG ]; then

    # Dump envs
    echo -e "\nDBMI DEBUG: Dumping environment: \n"
    printenv

fi