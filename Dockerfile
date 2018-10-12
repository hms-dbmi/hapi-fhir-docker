FROM maven:3.5-jdk-8-alpine AS builder

# Set the HAPI-FHIR project to install
ARG HAPI_FHIR_SRC=hapi-fhir-3.x

# Set the version of the build
ARG DBMI_HAPI_FHIR_VERSION
ENV DBMI_HAPI_FHIR_VERSION=${DBMI_HAPI_FHIR_VERSION}

# Set the version of HAPI-FHIR to use
ARG HAPI_FHIR_VERSION=3.5.0
ENV HAPI_FHIR_VERSION=${HAPI_FHIR_VERSION}

# Enable the overlay by defining this argument
ARG HAPI_FHIR_PROFILE=overlay

# Enable or disable JWT
ARG JWT_AUTH_ENABLED=true
ENV JWT_AUTH_ENABLED=${JWT_AUTH_ENABLED}

# Fetch HAPI-FHIR source and build the app
WORKDIR /usr/src/app/fhir-server

# Add the POM
COPY ${HAPI_FHIR_SRC}/pom.xml /usr/src/app/fhir-server/
COPY ${HAPI_FHIR_SRC}/settings.xml /usr/src/app/fhir-server/

# Bring down packages first and cache them and save millions of minutes
RUN mvn verify clean --fail-never -s /usr/src/app/fhir-server/settings.xml -P ${HAPI_FHIR_PROFILE}

# Copy our own source files over next
COPY ${HAPI_FHIR_SRC} /usr/src/app/fhir-server

# Build the final WAR
RUN mvn package -s /usr/src/app/fhir-server/settings.xml -P ${HAPI_FHIR_PROFILE}

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

# Copy scripts, templates and resources
ADD docker-entrypoint-templates.d/ /docker-entrypoint-templates.d/
ADD docker-entrypoint-resources.d/ /docker-entrypoint-resources.d/
ADD docker-entrypoint-init.d/ /docker-entrypoint-init.d/
ADD docker-entrypoint.d/ /docker-entrypoint.d/

# Add the init script and make it executable
ADD docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod a+x /docker-entrypoint.sh

# Set the build env
ENV DBMI_ENV=prod

# Set app parameters
ENV DBMI_PARAMETER_STORE_PREFIX=dbmi.fhir.${DBMI_ENV}
ENV DBMI_PARAMETER_STORE_PRIORITY=true
ENV DBMI_AWS_REGION=us-east-1

# Set nginx and network parameters
ENV DBMI_LB=true
ENV DBMI_SSL=true
ENV DBMI_CREATE_SSL=true

ENV DBMI_HEALTHCHECK=true
ENV DBMI_HEALTHCHECK_PATH=/healthcheck
ENV DBMI_APP_HEALTHCHECK_PATH=/baseDstu3/metadata

# Set FHIR variables
ENV FHIR_SERVER_URL=https://${DBMI_APP_DOMAIN}/baseDstu3
ENV FHIR_SERVER_NAME="DBMI FHIR Server"

# Copy the WAR file from builder
RUN rm -rf $CATALINA_HOME/webapps/ROOT
COPY --from=builder /usr/src/app/fhir-server/target/fhir-server.war $CATALINA_HOME/webapps/ROOT.war

ENTRYPOINT ["dumb-init", "/docker-entrypoint.sh"]

CMD $CATALINA_HOME/bin/catalina.sh run