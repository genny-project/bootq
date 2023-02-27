#!/bin/bash
set -e

KEYCLOAK_RESPONSE=`curl -s -X POST https://keycloak-testing.gada.io/auth/realms/${1}/protocol/openid-connect/token  -H "Content-Type: application/x-www-form-urlencoded" -d 'username=service' --data-urlencode "password=${4}" -d 'grant_type=password' -d "client_secret=${2}" -d "client_id=${3}"`

ACCESS_TOKEN=`echo "$KEYCLOAK_RESPONSE" | jq -r '.access_token'`
echo ${ACCESS_TOKEN}
