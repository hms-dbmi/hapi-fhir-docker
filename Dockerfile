FROM maven:3.5-jdk-8-alpine AS builder

# Set the HAPI-FHIR project to install
ARG HAPI_FHIR_SRC

# Set the version of HAPI-FHIR to use
ARG HAPI_FHIR_VERSION

# Fetch HAPI-FHIR source and build the app
WORKDIR /usr/src/app/fhir-server

# Add the POM
COPY ${HAPI_FHIR_SRC}/pom.xml /usr/src/app/fhir-server/
COPY ${HAPI_FHIR_SRC}/settings.xml /usr/src/app/fhir-server/

# Bring down packages first and cache them and save millions of minutes
RUN mvn verify clean --fail-never -s /usr/src/app/fhir-server/settings.xml

# Copy our own source files over next
COPY ${HAPI_FHIR_SRC} /usr/src/app/fhir-server

# Build the final WAR
RUN mvn package -s /usr/src/app/fhir-server/settings.xml

FROM tomcat:8-alpine

# Install needed packages
RUN addgroup -S nginx \
	&& adduser -D -S -h /var/cache/nginx -s /sbin/nologin -G nginx nginx \
	&& apk -Uuv add \
    nginx \
    curl \
    jq \
    python3 \
    py3-pip \
    openssl \
    dumb-init \
    && pip3 install awscli shinto-cli \
    && rm -rf /var/cache/apk/*

# Copy templates
ADD docker-entrypoint-templates.d/ /docker-entrypoint-templates.d/

# Setup entry scripts
ADD docker-entrypoint-init.d/ /docker-entrypoint-init.d/
ADD docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod a+x /docker-entrypoint.sh

# Set the build env
ENV PPM_ENV=prod

# Set app parameters
ENV PPM_PARAMETER_STORE_PREFIX=ppm.fhir.${PPM_ENV}
ENV PPM_PARAMETER_STORE_PRIORITY=true
ENV PPM_AWS_REGION=us-east-1
ENV PPM_APP_DOMAIN=ppm-fhir-prod.aws.dbmi.hms.harvard.edu

# Set nginx and network parameters
ENV PPM_NGINX_USER=nginx
ENV PPM_NGINX_PID_PATH=/var/run/nginx.pid
ENV PPM_PORT=443
ENV PPM_LB=true
ENV PPM_SSL=true
ENV PPM_CREATE_SSL=true
ENV PPM_SSL_PATH=/etc/nginx/ssl

ENV PPM_HEALTHCHECK=true
ENV PPM_HEALTHCHECK_PATH=/healthcheck
ENV PPM_APP_HEALTHCHECK_PATH=/baseDstu3/metadata

# Set FHIR variables
ENV FHIR_SERVER_URL=https://${PPM_APP_DOMAIN}/baseDstu3
ENV FHIR_SERVER_NAME="PPM FHIR Server"

# Copy the WAR file from builder
RUN rm -rf $CATALINA_HOME/webapps/ROOT
COPY --from=builder /usr/src/app/fhir-server/target/fhir-server.war $CATALINA_HOME/webapps/ROOT.war

ENTRYPOINT ["dumb-init", "/docker-entrypoint.sh"]