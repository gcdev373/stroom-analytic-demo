#!/bin/bash

export STROOM_SPARK_DATASOURCE_VERSION='2.0-alpha.1'

mkdir -p ../tmp/lib

export JAR="stroom-spark-datasource-${STROOM_SPARK_DATASOURCE_VERSION}-all.jar"
curl https://github.com/gchq/stroom-spark-datasource/releases/download/${STROOM_SPARK_DATASOURCE}/${JAR} > ../tmp/lib/${JAR}

cd ../demonstrator/analytics

source ../../bash/useNotebookWithPyspark.sh

echo $PYSPARK_DRIVER_PYTHON $PYSPARK_DRIVER_PYTHON_OPTS

SPARK_VERSION=2.4.8

pyspark --jars ../../tmp/lib/${JAR} --packages org.apache.spark:spark-sql-kafka-0-10_2.11:${SPARK_VERSION},org.apache.kafka:kafka-clients:0.10.0.1

