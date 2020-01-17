if [ -z "$STROOM_API_KEY"]
then
  echo "Please set STROOM_API_KEY environmental variable before starting"
  exit 1
fi

echo "This script must be run from a Java 8 shell"
echo "Please run from the bash directory i.e. so use the command ./startJupyter.sh to launch.  You must have previously installed jupyter e.g. by running the command:"
echo "sudo pip3 install juypter"
echo "or equivalent locally or within virtual environment."
echo "This script expects you to have previously cloned the stroom-spark-datasource repo into the same directory into which you have cloned this repo.  You must then have run the command:"
echo "gradle fatJar"

export PYSPARK_DRIVER_PYTHON=jupyter
export PYSPARK_DRIVER_PYTHON_OPTS='notebook'

SPARK_KAFKA_VERSION=0.10
DATASOURCE_VERSION=v1.0-alpha.4
BUILD_LIB='../../stroom-spark-datasource/build/libs'
FAT_JAR=$BUILD_LIB/stroom-spark-datasource-${DATASOURCE_VERSION}-all.jar

pyspark --jars $FAT_JAR --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.3,org.apache.kafka:kafka-clients:0.10.0.1


