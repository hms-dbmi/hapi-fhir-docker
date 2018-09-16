# DBMI HAPI-FHIR Container

This container runs HAPI-FHIR on Tomcat, proxied by nginx. It is setup
to run as an AWS ECS container, retrieving run configurations from PS,
managing AuthN/AuthZ through Auth0 JWT, and is entirely specific to our internal
deployments.

## Build instructions

The following command can be used to build the container. The `HAPI_FHIR_VERSION`
build argument specifies the release of the HAPI-FHIR source to specify as dependency
versions in the pom.xml. The `HAPI_FHIR_SOURCE` argument specifies which Maven 
project directory should be copied to the image for building. Due to package
differences between HAPI-FHIR 2.x and 3.x.x, each project source must be separate.
Also included are projects that contain the HAPI-FHIR overlay for testing and
viewing FHIR resources through a web UI.

```
docker build . -t dbmi/hapi-fhir:2.5-overlay --build-arg HAPI_FHIR_VERSION 2.5 --build-arg HAPI_FHIR_SOURCE hapi-fhir-2.x-overlay
```

## Running instructions

This project is built to persist data to a MySQL instance and then
authenticate and authorize by JWT. Details for both are configurable
through environment variables, including the ability to disable all JWT
AuthN/AuthZ if needed. Also needed is a port to set nginx to listen on
as well as a server name and absolute server URL for FHIR to correctly
return resource URLs despite being proxied by nginx.

```
docker run -d -e APP_PORT=8080 \
    -e FHIR_MYSQL_URL=<url> \
    -e FHIR_MYSQL_USERNAME=<username> \
    -e FHIR_MYSQL_PASSWORD=<password> \
    -e JWT_ISSUER=https://<client>.auth0.com/ \
    -e JWT_AUDIENCE=<Auth0 client ID> \
    -e JWT_HEADER_PREFIX="JWT " \
    -e JWT_COOKIE_NAME="DBMI_JWT \
    -e JWT_AUTHZ_CLAIM=https://some.oidc.compliant.namespace/authz \
    -e JWT_ADMIN_GROUP=fhir-admin \
    --name hapi-fhir dbmi/hapi-fhir:2.5-overlay
```