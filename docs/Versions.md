# Versioning Information
The demonstrator is dependent upon specific versions of libraries and other software, as detailed in this page.

## Stroom v7.0
Stroom v7.0 is required in order to provide the following features:
* StandardKafkaProducer
* AnnotationWriter
* Drilldown dashboard query condition assignment via parameter `expressionJson`

## Apache Spark v2.4.3
Newer versions of Spark have certain issues relating to Kafka integration, at time of writing.

The environmental variable `SPARK_HOME` should be set to the Spark v2.4.3 installation directory and `$PATH`  
should include `$SPARK_HOME/bin` 

## Python v3.7
Spark v2.4.3 is incompatible with Python v3.8+ 
