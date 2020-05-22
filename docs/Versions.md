# Versioning Information
The demonstrator is dependent upon specific versions of libraries and other software, as detailed in this page.

## Stroom v7.0
Stroom v7.0 is required in order to provide the following features:
* StandardKafkaProducer
* AnnotationWriter
* Drilldown dashboard query condition assignment via parameter `expressionJson`
* Compatibility with format of included Stroom configuration files (Stroom content packs).

## Apache Spark v2.4.3
Newer versions of Spark have certain issues relating to Kafka integration, at time of writing.

Use the `spark-2.4.3-bin-hadoop2.7` distribution, available from [Apache's archive](https://archive.apache.org/dist/spark/spark-2.4.3)

The environmental variable `SPARK_HOME` should be set to the Spark v2.4.3 installation directory 
and `$PATH`  should include `$SPARK_HOME/bin` 

## Python v3.7
Spark v2.4.3 is incompatible with Python v3.8+.

## Java 8 (1.8)
Spark v2.4.3 requires Java 8.  [Sdkman](https:/sdkman.io) or similar can be used to easily switch between jdk versions.

N.B. Java 12 (1.12 ) is required by Stroom v7.