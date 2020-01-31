#!/bin/bash

DATA_DIR="../../tmp/alerts"
INPUT_SUFFIX="csv"
FEED="SAMPLE-ALERTS"
OUTPUT_SUFFIX="complete"


echo "This will periodically send all alerts created by StateMonitor and similar analytics to Stroom."
sleep 2
echo "Reading alerts from files matching $DATA_DIR/*.$INPUT_SUFFIX and writing to feed $FEED"
echo "Please Wait..."
sleep 5

while ( true )
do
  for INPUT_FILE in `ls $DATA_DIR/*.$INPUT_SUFFIX 2> /dev/null`
  do
    mv $INPUT_FILE $INPUT_FILE.$RANDOM.$OUTPUT_SUFFIX 2> /dev/null 
  done
  echo Waiting for analytics to finish writing...
  sleep 10

  for FILE in `ls $DATA_DIR/*.$OUTPUT_SUFFIX 2> /dev/null`
  do
    echo Sending $FILE
    RESPONSE=`curl -w '%{http_code}' -k --data-binary @${FILE} "http://localhost:8080/stroom/noauth/datafeed" -H "Feed:$FEED"`
    if (( RESPONSE == 200 ))
    then
      echo "INFO: Alert batch successfully sent to Stroom."
      rm $FILE
    else
      echo "ERROR: Got $RESPONSE sending $FILE alert file to stroom."
    fi
  done
  echo Sleeping...
  sleep 120
done
