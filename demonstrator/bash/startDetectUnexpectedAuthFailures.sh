echo "This must be run in a Java 8 shell"
echo "Run this script from the demonstrator/bash directory (./)"

if [ -z "$SPARK_HOME" ]
then
  echo "Please set SPARK_HOME environmental variable before starting"
  exit 1
fi

export PYTHONPATH=$PYTHONPATH:"$SPARK_HOME/python/lib/*"

export SPARK_MAJOR_VERSION=2

cd ../analytics

export SPARK_VERSION=2.4.8

$SPARK_HOME/bin/spark-submit --packages org.apache.spark:spark-sql-kafka-0-10_2.11:${SPARK_VERSION},org.apache.kafka:kafka-clients:0.10.0.1  python/detectUnexpectedAuthFailures.py
