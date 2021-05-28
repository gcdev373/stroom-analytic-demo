# Versioning Information
The demonstrator is dependent upon specific versions of libraries and other software, as detailed in this page.

## Stroom v7.0
Stroom v7.0 is required in order to provide the following features:
* StandardKafkaProducer
* AnnotationWriter
* Drilldown dashboard query condition assignment via parameter `expressionJson`
* Compatibility with format of included Stroom configuration files (Stroom content packs).

## Apache Spark v2.4.x
It is necessary to use a compatible version of Apache Spark.  It has been tested with `2.4.3` and `2.4.8`, but other
`2.4.x` versions are also expected to work. 

Use the `spark-2.4.x-bin-hadoop2.7` distribution.

The environmental variable `SPARK_HOME` should be set to the Spark v2.4.3 installation directory 
and `$PATH`  should include `$SPARK_HOME/bin` 

## Python v3.7
Spark v2.4.x is incompatible with Python v3.8+.

This means that the python3 that is now standard on many Linux distributions is incompatible.  It is recommended
that [Pyenv](https://github.com/pyenv/pyenv) or similar is used to allow switching between the OS native version
of python and the one required by Spark.

You are recommended to create a virtual environment for the python associated with this demonstrator.

# Python Configuration
It is necessary to install the following python modules or make them available within your virtual environment:
* numpy
* pandas

## Jupyter Notebook Setup
In order to run all the Jupyter notebook dashboards included within the demonstrator, it is necessary to configure
notebook to run within the Spark context.


This can be achieved by setting certain environmental variables before starting `pyspark`. 
These lines should not be added to a `.bashrc` because this will prevent `pyspark` from starting without Jupyter, and
will prevent non-interactive analytics from working correctly.  

The script `bash/useNotebookWithPyspark.sh` may be sourced in the current shell to set these environmental variables,
prior to running pyspark within Jupyter notebook. 
```
. bash/useNotebookWithPyspark.sh
```

## Java 8 (1.8)
Spark v2.4.3 requires Java 8.  [Sdkman](https:/sdkman.io) or similar can be used to easily switch between jdk versions.

N.B. Java 12 (1.12 ) is required by Stroom v7.