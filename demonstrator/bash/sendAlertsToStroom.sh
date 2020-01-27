#!/bin/bash

DATA_DIR="../../tmp"
LOG_FILE="$DATA_DIR/statemonitor-ueba.csv"
FEED="SAMPLE-ALERTS"
SUFFIX="complete"


echo "This will periodically send all alerts created by StateMonitor to Stroom."
sleep 2
echo "Reading alerts from $LOG_FILE and writing to feed $FEED"
echo "Please Wait..."

while ( true )
do
  sleep 120
  mv $LOG_FILE $LOG_FILE.$RANDOM.$SUFFIX 2> /dev/null 
  for FILE in `ls $DATA_DIR/*.$SUFFIX 2> /dev/null`
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
done
