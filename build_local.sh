#!/bin/bash

# Set the build
VERSION="0.3.1"

# Check for arguments
if [[ -n $1 ]]; then

    # Set the profile to use
    PROFILE=${2:-default}

    # Build the tag
    if [[ $PROFILE == 'default' ]]; then
        TAG="$1"
    else
        TAG="$1-overlay"
    fi

    # Set the src based on the profile
    if [[ $1 =~ ^2.[0-9] ]]; then
        SRC=hapi-fhir-2.x
    else
        SRC=hapi-fhir-3.x.x
    fi

    # Build it
    docker build --build-arg DBMI_HAPI_FHIR_VERSION="${VERSION}" --build-arg HAPI_FHIR_PROFILE=$PROFILE --build-arg HAPI_FHIR_SRC="$SRC" --build-arg HAPI_FHIR_VERSION="$1" -t hmsdbmitc/ppm-fhir:"$TAG" .

else

    # Build 3.x.x versions
    VERSIONS_3="3.6.0,3.5.0,3.4.0,3.3.0,3.2.0,3.1.0,3.0.0"
    for v in ${VERSIONS_3//,/ }
    do
        # Build and push
        docker build --build-arg DBMI_HAPI_FHIR_VERSION="${VERSION}" --build-arg HAPI_FHIR_PROFILE=default --build-arg HAPI_FHIR_SRC="hapi-fhir-3.x.x" --build-arg HAPI_FHIR_VERSION="${v}" -t hmsdbmitc/ppm-fhir:${v} .

        # Build and push overlay version
        docker build --build-arg DBMI_HAPI_FHIR_VERSION="${VERSION}" --build-arg HAPI_FHIR_PROFILE=overlay --build-arg HAPI_FHIR_SRC="hapi-fhir-3.x.x" --build-arg HAPI_FHIR_VERSION="${v}" -t hmsdbmitc/ppm-fhir:${v}-overlay .
    done

    # Build 2.x versions
    VERSIONS_2="2.5,2.4"
    for v in ${VERSIONS_2//,/ }
    do
        # Build and push
        docker build --build-arg DBMI_HAPI_FHIR_VERSION="${VERSION}" --build-arg HAPI_FHIR_PROFILE=default --build-arg HAPI_FHIR_SRC="hapi-fhir-2.x" --build-arg HAPI_FHIR_VERSION="${v}" -t hmsdbmitc/ppm-fhir:${v} .

        # Build and push overlay version
        docker build --build-arg DBMI_HAPI_FHIR_VERSION="${VERSION}" --build-arg HAPI_FHIR_PROFILE=overlay --build-arg HAPI_FHIR_SRC="hapi-fhir-2.x" --build-arg HAPI_FHIR_VERSION="${v}" -t hmsdbmitc/ppm-fhir:${v}-overlay .
    done

fi