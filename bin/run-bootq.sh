#!/bin/bash
DOMAIN=http://localhost:8480
TOKEN=`./gettoken-prod.sh`
echo $DOMAIN
echo $TOKEN
versionResult=`curl -s -X GET  --header 'Accept: text/plain'   "${DOMAIN}/bootq/version"`
echo "Version endpoint test result:" $versionResult
sheetid=1W9BRH6cTRNACGPH8cVJtgUlefdZzG9U-WMCj1Qw4L2k
batchloadingResult=`curl --connect-timeout 5 -s -X GET  --header 'Accept: text/plain' --header "Authorization: Bearer $TOKEN"  "${DOMAIN}/bootq/loadsheets/${sheetid}"`
echo "Batch loading endpoint test result:" $batchloadingResult
