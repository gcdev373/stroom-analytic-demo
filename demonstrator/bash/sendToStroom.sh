#!/bin/bash

SUFFIX='.txt'

for FILE in `ls *.txt`; do
  FEED=`basename -s $SUFFIX $FILE`
  curl -k --data-binary @${FILE} "https://localhost/stroom/datafeed" -H "Feed:${FEED}"
done


