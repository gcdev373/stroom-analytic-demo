# Single Event Analysis with Complex Analytics
It is often desirable to perform complex ad-hoc analysis against single events.

For example, it might be desirable to run the event data through a statistical model, in order to detect outliers.

## Design
This example uses the machine learning model previously created with [Jupyter notebook](jupyterAnalysis.md) and runs it as a standalone
Spark Structured streaming application driven by events in JSON format via Kafka.

N.B. This particular analytic actually operates on counts of events received every hour, rather than directly on individual events.

Outliers are recorded as CSV file output, which is itself sent to Stroom via the datafeed API for triage.

## Implementation

The application comprises a [single python file](../demonstrator/analytics/python/detectUnexpectedAuthFailures.py)
This is based on the Jupyter Notebook "Streaming Analysis", it reads the `event-logging` schema from a JSON file
`event-logging-v3.2.3.json`, then it loads the ML model trained with the Jupyter Notebook "Training" and finally
it subscribes to the Kafka topic to receive events in JSON format.



When hour intervals are identified as having a significantly greater number of authentication failures than predicted by the model,
a new CSV output file `tmp/alerts/hits-???.csv` is written.

## Starting the application
From a java 8 shell, the following commands can be used to start the application (requres Apache Spark 2.4.3 to be installed).

```shell script
cd demonstrator/bash
./startDetectUnexpectedAuthFailures.sh
```

## Preparing to send the alerts to Stroom
In a separate shell window, start either:
1. `./sendAlertsToStroomDetections.sh` - if `Detections` are preferred for triage, or
1. `./sendAlertsToStroomAnnoatations.sh` - if `Annotations` are preferred for triage, see [this page](analyticOutput.md) for further infomation.

This process will peridically collect new CSV files and send them to the appropriate feed of the Stroom datafeed API.

## Invoking the application
The Spark streaming application requires Kafka messages to be written to the topic.  This is achieved by sending some new events into 
Stroom, which are then processed into Events and then placed onto [Kafka](kafkaproducer.md).  