echo "This must be run in a Java 8 shell"
echo "Run this script from the demonstrator/bash directory (./)"

if [ -z "$SPARK_HOME" ]
then
  echo "Please set SPARK_HOME environmental variable before starting"
  exit 1
fi

$SPARK_HOME/bin/spark-submit --class stroom.analytics.statemonitor.StateMonitor --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.3,org.apache.kafka:kafka-clients:0.10.0.1 ../../state-monitor/build/libs/state-monitor-all.jar ../../state-monitor/src/test/resources/ueba-accelerated.yml 
