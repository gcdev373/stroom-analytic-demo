cd ../../tmp/eventgen

echo "This script should be executed shortly before you wish to use the events as the event timestamp is based on the current time"

java -cp ../../event-gen/build/libs/event-gen-all.jar stroom.analytics.demo.eventgen.EventAccelerator 45 0 DEMO-MAINFRAME-EVENTS.txt DEMO-VPN-EVENTS.txt

cd eventAccelerator

echo "Sleeping for 2 minutes before sending to Stroom"
sleep 120
date

../../../bash/sendAcceleratedFilesToStroom.sh 45
