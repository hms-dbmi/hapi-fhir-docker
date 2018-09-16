#!/bin/bash -e

# Check for AWS EC2 internal endpoint
if [[ -n $DBMI_LB ]]; then

    # Get the EC2 host IP
    export DBMI_EC2_HOST=$(curl -sL http://169.254.169.254/latest/meta-data/local-ipv4)
    export ALLOWED_HOSTS=$ALLOWED_HOSTS,$DBMI_EC2_HOST

    # Set the trusted addresses for load balancers to the current subnet
    DBMI_EC2_MAC=$(curl -sL http://169.254.169.254/latest/meta-data/mac)
    export DBMI_LB_SUBNET=$(curl -sL http://169.254.169.254/latest/meta-data/network/interfaces/macs/$DBMI_EC2_MAC/vpc-ipv4-cidr-blocks)

fi

# Check for self signed
if [[ -n "$DBMI_CREATE_SSL" ]]; then

    # Set defaults
    DBMI_SSL_PATH=${DBMI_SSL_PATH:=/etc/nginx/ssl}
    DBMI_APP_DOMAIN=${DBMI_APP_DOMAIN:=localhost}

    # Set the wildcarded domain we want to use
    commonname="*.${DBMI_APP_DOMAIN}"

    # Ensure the directory exists
    mkdir -p ${DBMI_SSL_PATH}

    # A blank passphrase
    passphrase="$(openssl rand -base64 15)"
    country=US
    state=Massachusetts
    locality=Boston
    organization=HMS
    organizationalunit=DBMI
    email=DBMI_admin@hms.harvard.edu

    # Generate our Private Key, CSR and Certificate
    openssl genrsa -out "${DBMI_SSL_PATH}/${DBMI_APP_DOMAIN}.key" 2048
    openssl req -new -key "${DBMI_SSL_PATH}/${DBMI_APP_DOMAIN}.key" -out "${DBMI_SSL_PATH}/${DBMI_APP_DOMAIN}.csr" -passin pass:${passphrase} -subj "/C=$country/ST=$state/L=$locality/O=$organization/OU=$organizationalunit/CN=$commonname/emailAddress=$email"
    openssl x509 -req -days 365 -in "${DBMI_SSL_PATH}/${DBMI_APP_DOMAIN}.csr" -signkey "${DBMI_SSL_PATH}/${DBMI_APP_DOMAIN}.key" -out "${DBMI_SSL_PATH}/${DBMI_APP_DOMAIN}.crt"

    # Also create a wildcard certificate for errant requests
    passphrase="$(openssl rand -base64 15)"
    commonname="*"
    country=US
    state=Massachusetts
    locality=Boston
    organization=Nothing
    organizationalunit=Default
    email=nothing@default.com
    openssl genrsa -out "${DBMI_SSL_PATH}/default.key" 2048
    openssl req -new -key "${DBMI_SSL_PATH}/default.key" -out "${DBMI_SSL_PATH}/default.csr" -passin pass:${passphrase} -subj "/C=$country/ST=$state/L=$locality/O=$organization/OU=$organizationalunit/CN=$commonname/emailAddress=$email"
    openssl x509 -req -days 365 -in "${DBMI_SSL_PATH}/default.csr" -signkey "${DBMI_SSL_PATH}/default.key" -out "${DBMI_SSL_PATH}/default.crt"

fi

# Setup the nginx and site configuration
j2 /docker-entrypoint-templates.d/nginx.healthcheck.conf.j2 > /etc/nginx/conf.d/nginx.healthcheck.conf
j2 /docker-entrypoint-templates.d/nginx.conf.j2 > /etc/nginx/nginx.conf