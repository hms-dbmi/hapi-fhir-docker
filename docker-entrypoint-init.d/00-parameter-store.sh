#!/bin/bash -e

# Set the default region
export AWS_DEFAULT_REGION=${DBMI_AWS_REGION:=us-east-1}

get_prefix_params() {
  local response
  local params
  local secrets
  local next_token

  response=$(
    aws ssm describe-parameters  \
      --parameter-filters Key=Name,Option=BeginsWith,Values=${DBMI_PARAMETER_STORE_PREFIX} \
      --max-items 10 \
      "$@"
  )
  params=$(echo "$response" | jq -r '.Parameters[]')

  secrets=$(
    aws ssm get-parameters \
      --with-decryption \
      --names \
        $(echo "$response" | jq -r '.Parameters[].Name') \
        | jq -r '.Parameters[] | {(.Name | split(".") | last | ascii_upcase): .Value} | to_entries | .[]'
  )

  printf '%s\n' "$secrets"

  next_token=$(echo "$response" | jq -re '.NextToken // empty')
  if [ ! -z "$next_token" ]; then
    get_prefix_params --starting-token "$next_token"
  fi
}

get_path_params() {
  local response
  local secrets
  local next_token

  response=$(
    aws ssm get-parameters-by-path \
      --with-decryption \
      --recursive \
      --path ${DBMI_PARAMETER_STORE_PATH} \
      --max-items 50 \
      "$@"
  )

  secrets=$(echo "$response" | jq -r 'try .Parameters[] | {(.Name | split("/") | last | ascii_upcase): .Value} | to_entries | .[]')

  printf '%s\n' "$secrets"

  next_token=$(echo "$response" | jq -re '.NextToken // empty')
  if [ ! -z "$next_token" ]; then
    get_path_params --starting-token "$next_token"
  fi
}

params_to_env () {

    # Check the query type
    case "$1" in
    /*)
        eval `get_path_params | jq -r '"export \(.key)=\"\(.value)\""'`
        ;;
    *)
        eval `get_prefix_params | jq -r '"export \(.key)=\"\(.value)\""'`
        ;;
    esac
}

params_to_env_no_overwrite () {

    # Check the query type
    case "$1" in
    /*)
        eval `get_path_params | jq -r '"export \(.key)={\(.key):-"\(.value)\"}"'`
        ;;
    *)
        eval `get_prefix_params | jq -r '"export \(.key)={\(.key):-\"\(.value)\"}"'`
        ;;
    esac
}

# Check environment for path or prefix or both
if [[ -n $DBMI_PARAMETER_STORE_PREFIX ]]; then

    (>&2 echo "Getting secrets for prefix: $DBMI_PARAMETER_STORE_PREFIX")
    if [[ -n $DBMI_PARAMETER_STORE_PRIORITY ]]; then
        # Run it
        params_to_env $DBMI_PARAMETER_STORE_PREFIX
    else
        # Run
        (>&2 echo "Will preserve existing environment over values from Parameter Store")
        params_to_env_no_overwrite $DBMI_PARAMETER_STORE_PREFIX
    fi
fi

# Check environment for path or prefix or both
if [[ -n $DBMI_PARAMETER_STORE_PATH ]]; then

    (>&2 echo "Getting secrets for path: $DBMI_PARAMETER_STORE_PATH")
    if [[ -n $DBMI_PARAMETER_STORE_PRIORITY ]]; then
        # Run it
        params_to_env $DBMI_PARAMETER_STORE_PATH
    else
        # Run
        (>&2 echo "Will preserve existing environment over values from Parameter Store")
        params_to_env_no_overwrite $DBMI_PARAMETER_STORE_PATH
    fi
fi

if [[ -z $DBMI_PARAMETER_STORE_PATH && -z $DBMI_PARAMETER_STORE_PREFIX ]]; then

    (>&2 echo "No path or prefix specified, nothing to do")

fi