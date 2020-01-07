#!/bin/bash

SUFFIX='.txt'

if (( $# < 1 ))
then
  echo "Usage: $0 <number of seconds per batch>"
  exit 1
fi

if ! [[ $1 =~ ^[0-9]{1,4}$ ]]
then
  echo "Usage: $0 <number of seconds per batch>"
  echo "Usage: Number of seconds per batch must be a small integer. Got $1"
  exit 1
fi

let MAX_BATCH_NUM=`ls *.txt | cut -d . -f 1 | sort -nu | tail -1`

for ((BATCH=1; BATCH <= MAX_BATCH_NUM; BATCH++))
do
  echo "Processing Batch $BATCH of $MAX_BATCH_NUM"
  for FILE in `ls $BATCH.*.txt`
  do
    FEED=`basename -s $SUFFIX $FILE | cut -d . -f 2-`
    curl -k --data-binary @${FILE} "http://localhost:8080/stroom/noauth/datafeed" -H "Feed:${FEED}"
    echo curl -k --data-binary @${FILE} "http://localhost:8080/stroom/noauth/datafeed" -H "Feed:${FEED}"
  done
  echo "Sleeping for $1 seconds, please wait..."
  sleep $1
  echo
done


