# Tl;Dr
People learn in different ways.  If you like to start by seeing things working, the following steps are suggested
(assumes some familiarity with Stroom):
1. Run `./buildall.sh` from `bash/` directory to compile the software.
1. Install the `stroom-full` stack for [Stroom v7](https://github.com/gchq/stroom/releases/latest)
1. Enable the correct services by running ` ./set_services.sh stroom stroom-all-dbs kafka zookeeper nginx`
1. Start required services from the stack by running `start.sh`
1. Log into the Stroom UI as `admin` (default password is `admin`).  Select a new password.
1. Import content pack `demonstrator/stroom/StroomConfig-all.zip` from `demonstrator/stroom` via Stroom UI
1. Create a [python 3.7](Versions.md#python-v37) virtual environment and switch into it.
1. Use `pip` to install `numpy` and `pandas` in your virtual environment.
1. Install [Apache Spark 2.4.8](Versions.md#apache-spark-v24x)
1. Ensure that `$SPARK_HOME` is set to the Spark v2.4.8 installation directory and `$PATH` includes `$SPARK_HOME/bin`
1. Open a terminal using your virtual environment; select a Java 1.8 jdk (e.g. by using [sdkman](https://sdkman.io/) ; cd into `demonstrator/bash` and run `./startDetectUnexpectedAuthFailures.sh`
1. Open another terminal; cd into `demonstrator/bash` and run `./sendAlertsToStroom.sh`
1. Open yet another terminal; cd into `demonstrator/analytics/data/eventgen` and run command `../../../bash/sendToStroom.sh`
1. Wait for Stroom to complete its processing
1. Open dashboard `Streaming Analytic Output` and to see outliers as determined against a previously trained ML model.

# Wait! What Happened?
The above steps have mostly been about installing and setting up software.

However, the interesting bit is the time series analytic itself, which illustrates
a design pattern for near real-time alerting, and a workflow that could be used for more complex time-series analysis.  

[Read more...](SingleEventComplexAnalysisWalkthrough.md)