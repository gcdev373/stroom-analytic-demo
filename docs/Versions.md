# Versioning Information
The demonstrator is dependent upon specific versions of libraries and other software, as detailed in this page.

## Apache Spark v2.4.3
Newer versions of Spark have certain issues relating to Kafka integration, at time of writing.

The environmental variable `SPARK_HOME` should be set to the Spark v2.4.3 installation directory and `$PATH`  
should include `$SPARK_HOME/bin` 

## Python v3.7
Spark v2.4.3 is incompatible with Python v3.8+ 
