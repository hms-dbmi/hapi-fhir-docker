#!/bin/bash -e

# Check for AWS EC2 internal endpoint
if [[ -n $PPM_LB ]]; then

    # Get the EC2 host IP
    export PPM_EC2_HOST=$(curl -sL http://169.254.169.254/latest/meta-data/local-ipv4)
    export ALLOWED_HOSTS=$ALLOWED_HOSTS,$PPM_EC2_HOST

    # Set the trusted addresses for load balancers to the current subnet
    PPM_EC2_MAC=$(curl -sL http://169.254.169.254/latest/meta-data/mac)
    export PPM_LB_SUBNET=$(curl -sL http://169.254.169.254/latest/meta-data/network/interfaces/macs/$PPM_EC2_MAC/vpc-ipv4-cidr-blocks)

fi

# Check for self signed
if [[ -n "$PPM_CREATE_SSL" ]]; then

    # Set defaults
    PPM_SSL_PATH=${PPM_SSL_PATH:=/etc/nginx/ssl}
    PPM_APP_DOMAIN=${PPM_APP_DOMAIN:=localhost}

    # Set the wildcarded domain we want to use
    commonname="*.${PPM_APP_DOMAIN}"

    # Ensure the directory exists
    mkdir -p ${PPM_SSL_PATH}

    # A blank passphrase
    passphrase="$(openssl rand -base64 15)"
    country=US
    state=Massachusetts
    locality=Boston
    organization=HMS
    organizationalunit=DBMI
    email=ppm_admin@hms.harvard.edu

    # Generate our Private Key, CSR and Certificate
    openssl genrsa -out "${PPM_SSL_PATH}/${PPM_APP_DOMAIN}.key" 2048
    openssl req -new -key "${PPM_SSL_PATH}/${PPM_APP_DOMAIN}.key" -out "${PPM_SSL_PATH}/${PPM_APP_DOMAIN}.csr" -passin pass:${passphrase} -subj "/C=$country/ST=$state/L=$locality/O=$organization/OU=$organizationalunit/CN=$commonname/emailAddress=$email"
    openssl x509 -req -days 365 -in "${PPM_SSL_PATH}/${PPM_APP_DOMAIN}.csr" -signkey "${PPM_SSL_PATH}/${PPM_APP_DOMAIN}.key" -out "${PPM_SSL_PATH}/${PPM_APP_DOMAIN}.crt"

fi

# Setup the nginx and site configuration
j2 /docker-entrypoint-templates.d/nginx.conf.j2 > /etc/nginx/nginx.conf

# Check if nginx is running, reload it or start it
if [ -e $PPM_NGINX_PID_PATH ]; then
    nginx -s reload
else
    nginx
fi