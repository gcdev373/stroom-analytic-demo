#!/bin/bash

DATA_DIR="../../tmp/alerts"
INPUT_SUFFIX="csv"
DETECTIONS_FEED="SAMPLE-DETECTIONS"
ANNOTATIONS_FEED="SAMPLE-ALERTS"
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
  # echo Waiting for analytics to finish writing...
  sleep 10

  for FILE in `ls $DATA_DIR/*.$OUTPUT_SUFFIX 2> /dev/null`
  do
    echo Sending $FILE
    RESPONSE1=`curl -w '%{http_code}' -k --data-binary @${FILE} "http://localhost:8080/stroom/noauth/datafeed" -H "Feed:$DETECTIONS_FEED"`
    RESPONSE2=`curl -w '%{http_code}' -k --data-binary @${FILE} "http://localhost:8080/stroom/noauth/datafeed" -H "Feed:$ANNOTATIONS_FEED"`

    if (( RESPONSE1 == 200  && RESPONSE2 == 200 ))
    then
      echo "INFO: Alert batch successfully sent to Stroom."
      rm $FILE
    else
      echo "ERROR: Got $RESPONSE1 sending $FILE alert file to stroom on feed $DETECTIONS_FEED and $RESPONSE2 sending to feed $ANNOTATIONS_FEED"
      echo "Please correct the problem and then restart this script"
      exit 1
    fi
  done
  #echo Sleeping...
  sleep 120
done
