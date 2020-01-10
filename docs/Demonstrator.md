# Analytic Demonstrator
## Overview
This analytic demonstrator comprise the following components:
1. **EventGen** - a self-contained stochastic event generator that creates text files
1. **Stroom** - Modified version of Stroom that provides enhanced support for Kafka
1. **Stroom content** - Feeds, XSLT, XSD and other configuration required to configure Stroom to accept EventGen data
and write it to a Kafka topic.
1. **Analytic content** - Kafka analytics, python code, etc.

In addition, the following components are also required:
1. Kafka - typically deployed to the same host as Stroom via docker.
1. Zookeeper - required by Kafka.
1. Supporting Stroom components - these are typically deployed to the same host as Stroom via docker.

## EventGen
This is a Java application that creates events in text files.  It is not necessary to process these files using Stroom, so 
CSV format is used, making it easy to process with any technology.

The events generated are determined by a stochastic (random) process to drive a finite state machine, 
all of which is controlled via a `yaml` format config file.
This configuration file also controls all other aspects of the `EventGen` application, including the set of files created.  

Usage: `java -jar event-gen-all.jar <path to config yaml>`

The demonstrator includes a configuration file called `ueba.yml`.  Running `EventGen` with this configuration results in
the creation of files in the default location (`/tmp/eventgen`) The following files are produced:
* DEMO-MAINFRAME-EVENTS (43589 lines)
* DEMO-VPN-EVENTS (10892 lines)
* special.out (10 lines)

The first two files contain event streams that are intended to be representative of those that could be created by 
real-world event (log) sources.  Therefore, it is these that are expected to be analysed by an analytic process.

The last file (`special.out`) is far smaller than the other files, it records events that are the result of rare
state transitions, and it is expected that these may be what an analytic process might identify.  In effect, this file
contains the "answers", to a number of as yet unposed questions.

#### Uploading to Stroom
It is necessary to upload the resultant `.txt` files to Stroom for processing.  The HTTP header `Feed` should be set correctly.
The example configuration file `ueba.yml` creates files that have a filename that corresponds to the appropriate Stroom feed.

A script `demonstrator/bash/sendToStroom.sh` is provided to achieve this.  It should be run from the directory containing
the output from `EventGen`, for example:
```Shell
cd /tmp/eventgen
~/git/stroom-analytic-demo/demonstrator/bash/sendToStroom.sh
```

Where `~/git` should be replaced with the local directory location where you cloned this repo.

Alternatively, it is possible to manually upload the files created by `EventGen` to the appropriate Stroom feeds via the
Stroom UI.

## Stroom
The modified version of stroom provides the pipeline filter element `StandardKafkaProducer`.  This expects to receive
events in the form of `kafka-records` XML documents.

These records are placed onto Kafka using the producer properties supplied to the filter.

Every `kafka-record` can specify:
* The kafka **topic** to be targeted
* The kafka **partition** that will be used
* The message **key**
* The message **timestamp** that will be used
* Any kafka **headers** that may be required
* The message **value** (i.e. the message itself)

## Stroom content
The following content is included:
1. **kafka-records** *Stroom XML schema definition* that defines the format required by `StandardKafkaProducer`
1. **ANALYTIC-ALERTS** *Stroom feed definition* used to receive alerts detected by analytics, for storage and/or follow-on analysis within Stroom.
1. **DEMO-MAINFRAME-EVENTS** *Stroom feed definition* used to receive the file of this name created by `EventGen`
1. **DEMO-VPN-EVENTS** *Stroom feed definition* used to receive the file of this name created by `EventGen`
1. **DEMO-EVENTS** *Stroom pipeline definition* used to process CSV `EventGen` data into `event-logging` XML docs.
1. **DEMO-EVENTS** *Stroom XSLT Translation* converts CSV `EventGen` data into `event-logging` XML docs.
1. **CSV splitter no header** *Stroom Text Converter definition* used by `DEMO-EVENTS` Pipeline.
1. **Sample Index** *Stroom Index definition* to enable interactive analysis of `EventGen` data.
1. **Sample Index** *Stroom pipeline definition* to populate the `Sample Index` with `EventGen` data.
1. **Sample Index** *Stroom XSLT Translation* converts `event-logging` XML data into `records` XML docs as required for index creation.
1. **Sample Topic** *Stroom pipeline definition* to create Kafka events representing `EventGen` data. 
1. **Sample Topic** *Stroom XSLT definition* converts `event-logging` XML data into `kafka-records` as required for Kafka record creation.
1. **Sample Producer** *Stroom Kafka Configuration definition* defines how and where the kafka messages are to be written.

#### Features
It is desirable that analytics that are developed interactively, using searches against the index can later be
converted into streaming analytics.  In order to facilitate this process, the fields of the index are duplicated
as Kafka headers.  `Sample Topic` XSLT contains the directive `xsl:include("Sample Index")` in order to duplicate 
this logic whilst preventing duplication of XSLT code. 

### Processing the data
Using the Stroom UI, processor filters should be created on the following pipelines:
1. **DEMO-EVENTS** to convert all `Raw Events` streams on feeds `DEMO-MAINFRAME-EVENTS` and `DEMO-VPN-EVENTS` into `event-logging` XML.
1. **Sample Index** to place all `Events` streams on feeds `DEMO-MAINFRAME-EVENTS` and `DEMO-VPN-EVENTS` into the index.
1. **Sample Topic** to place all `Events` streams on feeds `DEMO-MAINFRAME-EVENTS` and `DEMO-VPN-EVENTS` onto the topic.

Alternatively, it is possible to do this directly via the database.  

This may save time in a test environment.
 
Assuming that mysql is running in a docker container on `localhost`,
it is possible to connect to it using the following command:
```Shell
docker exec -it stroom-all-dbs mysql -h"localhost" -P"3307" -u"stroomuser" -p"stroompassword1" stroom
```
Replacing the password as necessary for your environment.

**N.B. This will enable all processors and processor filters.  Only do this on a standalone/test instance of stroom**
```SQL
update processor set enabled = true;
update processor_filter set enabled = true;
```

# Next Steps
## Data Analysis
The data within Stroom can be analysed using [Jupyter Notebook](JupyterAnalysis.md)

## Streaming Analytics
For a more advanced example, the tools within this repo can be used to provide an end-to-end demonstration of [multiple event analysis](MultipleEventAnalysisWalkthrough.md)

