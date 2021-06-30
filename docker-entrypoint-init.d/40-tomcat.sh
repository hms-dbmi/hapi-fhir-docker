#!/bin/bash -e

# Check for self signed
if [[ "$DBMI_SSL" = "https" ]] && [[ -n "$DBMI_CREATE_SSL" ]]; then

  # A blank passphrase
  export DBMI_CATALINA_SSL_PASSWORD="$(openssl rand -base64 15)"
  country=US
  state=Massachusetts
  locality=Boston
  organization=HMS
  organizationalunit=DBMI
  email=DBMI_admin@hms.harvard.edu

  export DBMI_CATALINA_SSL_PATH=${DBMI_CATALINA_SSL_PATH:-$CATALINA_HOME/conf/ssl}
  DBMI_APP_DOMAIN=${DBMI_APP_DOMAIN:=localhost}

  # Ensure dir exists
  mkdir -p "${DBMI_CATALINA_SSL_PATH}"

  # Create a keystore
  $JAVA_HOME/bin/keytool -genkey \
    -noprompt -keyalg RSA -keysize 4096 -validity 720 \
    -alias ${DBMI_APP_DOMAIN}_csr \
    -dname "CN=$DBMI_APP_DOMAIN, OU=$organizationalunit, O=$organization, L=$locality, S=$state, C=$country" \
    -keystore "${DBMI_CATALINA_SSL_PATH}/${DBMI_APP_DOMAIN}.jks" \
    -storepass "${DBMI_CATALINA_SSL_PASSWORD}" \
    -keypass "${DBMI_CATALINA_SSL_PASSWORD}"

  # Export the self-signed certificate
  $JAVA_HOME/bin/keytool -export \
    -alias ${DBMI_APP_DOMAIN}_csr \
    -storepass "${DBMI_CATALINA_SSL_PASSWORD}" \
    -file "${DBMI_CATALINA_SSL_PATH}/${DBMI_APP_DOMAIN}.cer" \
    -keystore "${DBMI_CATALINA_SSL_PATH}/${DBMI_APP_DOMAIN}.jks"

  # Import it back as trusted into the keystore
  $JAVA_HOME/bin/keytool -import -v -noprompt -trustcacerts \
    -alias ${DBMI_APP_DOMAIN} \
    -file "${DBMI_CATALINA_SSL_PATH}/${DBMI_APP_DOMAIN}.cer" \
    -keystore "${DBMI_CATALINA_SSL_PATH}/${DBMI_APP_DOMAIN}.jks" \
    -storepass "${DBMI_CATALINA_SSL_PASSWORD}" \
    -keypass "${DBMI_CATALINA_SSL_PASSWORD}"

  # Also import it into the JRE's keystore to ensure it's trusted
  $JAVA_HOME/bin/keytool -importcert -noprompt \
    -file "${DBMI_CATALINA_SSL_PATH}/${DBMI_APP_DOMAIN}.cer" \
    -alias ${DBMI_APP_DOMAIN} \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass "changeit" \
    -keypass "changeit"

elif [[ "$DBMI_SSL" = "https" ]] && [[ -n "$DBMI_SSL_KEY" ]] && [[ -n "$DBMI_SSL_PKCS7" ]]; then

  # Dump key and certs to file
  echo "$DBMI_SSL_KEY" > private.key
  echo "$DBMI_SSL_PKCS7" > cert.p7b

  # Convert to PEM
  openssl pkcs7 -print_certs -in cert.p7b -out cert.pem

  # Convert to PKCS12
  openssl pkcs12 -export \
    -name ${DBMI_APP_DOMAIN} \
    -in cert.pem \
    -inkey private.key \
    -out cert.p12

  $JAVA_HOME/bin/keytool -importkeystore \
    -srcstoretype pkcs12 \
    -srckeystore cert.p12 \
    -destkeystore "${DBMI_CATALINA_SSL_PATH}/${DBMI_APP_DOMAIN}.jks"
fi

# Setup the nginx and site configuration
j2 /docker-entrypoint-templates.d/server.xml.j2 > $CATALINA_HOME/conf/server.xml

# Find template resource files in the deployed webapp and render them
# using runtime environment variables
find $CATALINA_HOME/webapps/ROOT/WEB-INF/ -type f -name "*.j2" -exec sh -c 'j2 "$0" > "${0%.*}"' {} \;