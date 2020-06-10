#!/bin/bash

# Set the build
VERSION="1.0.0"

# Check if specific version
if [[ -n "$1" ]]; then
  VERSIONS="$1"
else
  VERSIONS="5.0.2,5.0.1,5.0.0,4.2.0,4.1.0,4.0.1,4.0.0,3.8.0,3.7.0,3.6.0,3.5.0,3.4.0,3.3.0,3.2.0,3.1.0,3.0.0,2.5,2.4"
fi

# Build all version
for v in ${VERSIONS//,/ }
do
    # Get major, minor, patch
    if [[ ${v%%.*} = "2" ]]; then
      PATCH="0"
      MINOR=${v##*.}
      MAJOR=${v%%.*}
    else
      PATCH=${v##*.}
      MINOR=`TMP=${v%.*}; echo ${TMP##*.}`
      MAJOR=${v%%.*}
    fi

    # Build and push
    docker build \
      --build-arg DBMI_HAPI_FHIR_VERSION="${VERSION}" \
      --build-arg HAPI_FHIR_VERSION_MAJOR="${MAJOR}" \
      --build-arg HAPI_FHIR_VERSION_MINOR="${MINOR}" \
      --build-arg HAPI_FHIR_VERSION_PATCH="${PATCH}" \
      --build-arg HAPI_FHIR_VERSION="${v}" \
      -t hmsdbmitc/hapi-fhir:${v} .

    # Check if we should push it somewhere
    if [[ -n $2 ]]; then

      # Check if Amazon
      if [[ $2 ~= "amazonaws.com" ]]; then
        # Log int
        $(aws ecr get-login --no-include-email --region us-east-1)
      fi

      docker tag hmsdbmitc/hapi-fhir:${v} $2/hapi-fhir:${v}
      docker push $2/hapi-fhir:${v}
    fi
done