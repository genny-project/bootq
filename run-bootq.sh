#!/bin/bash
DOMAIN=http://localhost:8095
TOKEN=`./gettoken-prod.sh`
echo $DOMAIN
echo "TOKEN: " $TOKEN
versionResult=`curl -X GET  --header 'Accept: text/plain'   "${DOMAIN}/bootq/version"`
echo "Version endpoint test result:" $versionResult
sheetid=10oGZwKEmBNW_YdE_rqEhVgRwGT36P2Qb-ouB0uptriM
batchloadingResult=`curl --connect-timeout 5 -v -X GET  --header 'Accept: text/plain' --header "Authorization: Bearer $TOKEN"  "${DOMAIN}/bootq/loadsheets/${sheetid}"`
echo "Batch loading endpoint test result:" $batchloadingResult
