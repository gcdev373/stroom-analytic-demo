#!/bin/bash

SUFFIX='.txt'

echo "This script expects to be run from the directory containing the .txt files you wish to send"
sleep 3
for FILE in `ls *$SUFFIX`; do
  FEED=`basename -s $SUFFIX $FILE`
  curl -k --data-binary @${FILE} "http://localhost:8080/stroom/noauth/datafeed" -H "Feed:${FEED}"
# http http://localhost:8080/stroom/datafeed @${FILE} Feed:${FEED}
done


