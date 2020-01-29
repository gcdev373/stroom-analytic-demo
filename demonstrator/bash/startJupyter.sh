#!/bin/bash

if [ -z "$STROOM_API_KEY" ]
then
  echo "Please set STROOM_API_KEY environmental variable before starting"
  exit 1
fi

if [ -z "$STROOM_SPARK_DATASOURCE_FATJAR" ]
then
  echo "Please set STROOM_SPARK_DATASOURCE_FATJAR environmental variable to path to Stroom Spark Datasource Fat Jar file, e.g. /path/to/my/stroom-spark-datasource-v1.0-alpha.4-all.jar"
  exit 1
fi


echo "This script must be run from a Java 8 shell"
echo "Please run from the bash directory i.e. so use the command ./startJupyter.sh to launch.  You must have previously installed jupyter e.g. by running the command:"
echo "sudo pip3 install juypter"
echo "or equivalent locally or within virtual environment."

export PYSPARK_DRIVER_PYTHON=jupyter
export PYSPARK_DRIVER_PYTHON_OPTS='notebook'

SPARK_KAFKA_VERSION=0.10

cd ../analytics

pyspark --jars $STROOM_SPARK_DATASOURCE_FATJAR --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.3,org.apache.kafka:kafka-clients:0.10.0.1


