#!/bin/bash

SUFFIX='.txt'

echo "This script expects to be run from the directory containing the .txt files you wish to send"
echo
sleep 3
for FILE in `ls *$SUFFIX`; do
  FEED=`basename -s $SUFFIX $FILE`
  RESPONSE=`curl -w '%{http_code}' -k --data-binary @${FILE} "http://localhost:8080/stroom/noauth/datafeed" -H "Feed:$FEED"`

  echo Sent $FILE to Stroom feed $FEED with response code $RESPONSE
  echo
#  curl -k --data-binary @${FILE} "http://localhost:8080/stroom/noauth/datafeed" -H "Feed:${FEED}"
# http http://localhost:8080/stroom/datafeed @${FILE} Feed:${FEED}
done


