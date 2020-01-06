#!/bin/bash

SUFFIX='.txt'

for FILE in "ls $SUFFIX"; do
  FEED=`basename -s $SUFFIX $FILE`
  curl -k --data-binary @${FILE} "https://localhost/stroom/datafeed" -H "Feed:${FEED}"
# http http://localhost:8080/stroom/datafeed @${FILE} Feed:${FEED}
done


