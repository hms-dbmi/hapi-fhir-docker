#!/bin/bash

# Set the build
VERSION="0.3.1"

# Build 3.x.x versions
VERSIONS_3="3.6.0,3.5.0,3.4.0,3.3.0,3.2.0,3.1.0,3.0.0"
for v in ${VERSIONS_3//,/ }
do
    # Build and push
    docker build --build-arg DBMI_HAPI_FHIR_VERSION="${VERSION}" --build-arg HAPI_FHIR_PROFILE="${v}" --build-arg HAPI_FHIR_SRC="hapi-fhir-3.x.x" --build-arg HAPI_FHIR_VERSION="${v}" -t hmsdbmitc/ppm-fhir:${v} .

    # Build and push overlay version
    docker build --build-arg DBMI_HAPI_FHIR_VERSION="${VERSION}" --build-arg HAPI_FHIR_PROFILE="${v}-overlay" --build-arg HAPI_FHIR_SRC="hapi-fhir-3.x.x" --build-arg HAPI_FHIR_VERSION="${v}" -t hmsdbmitc/ppm-fhir:${v}-overlay .
done

# Build 2.x versions
VERSIONS_2="2.5,2.4"
for v in ${VERSIONS_2//,/ }
do
    # Build and push
    docker build --build-arg DBMI_HAPI_FHIR_VERSION="${VERSION}" --build-arg HAPI_FHIR_PROFILE="${v}" --build-arg HAPI_FHIR_SRC="hapi-fhir-2.x" --build-arg HAPI_FHIR_VERSION="${v}" -t hmsdbmitc/ppm-fhir:${v} .

    # Build and push overlay version
    docker build --build-arg DBMI_HAPI_FHIR_VERSION="${VERSION}" --build-arg HAPI_FHIR_PROFILE="${v}-overlay" --build-arg HAPI_FHIR_SRC="hapi-fhir-2.x" --build-arg HAPI_FHIR_VERSION="${v}" -t hmsdbmitc/ppm-fhir:${v}-overlay .
done