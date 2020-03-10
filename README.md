# stroom-analytic-demo
Stroom has powerful capabilities for collection, normalisation and storage of data.

However, in order to analyse this data, it is often necessary to integrate Stroom with an external anaytic framework.

This repo presents just one possible approach.

# Approach
This repo demonstrates how Apache Spark can be used to develop analytics and how Spark Structured Streaming can be
used to provide a near-real-time alerting capability by reading events from Apache Kafka that have been placed onto a
topic by Stroom.

The output from these analytics are fed back into Stroom in such a way as to support effective triage, alongside output
from simpler analytics that run within Stroom itself.

## Overview
The end-to-end process for near-real-time analysis is as follows (follow links for detail):
1. [Events are generated by an event source](docs/generation.md)
1. [A batch of events are sent into Stroom](docs/post.md)
1. [Stroom receives and stores the batch of events](docs/rawstream.md)
1. [Stroom normalises the batch of events by creating an additional event stream containing events in a standard format 
(typically `event-logging` format XML)](docs/normalisation.md)
1. [(Optional) Stroom indexes the events to support interactive searching and search via Search API](docs/indexing.md)
1. [Stroom converts the events into JSON format and writes these to a Kafka topic](docs/kafkaproducer.md)
1. [Applications within Apache Spark read events from Kafka and perform analysis.](docs/analysis.md) 
1. [Analytic output (e.g. alerts) are fed back into Stroom for triage.](docs/analyticOutput.md)
 
# Standalone Utilities

Although the repo is designed to demonstrate analysis with Stroom (using Spark and Kafka), it contains a number of utility applications that are not
coupled to Stroom and operate on plain text formats (CSV).  

These may be useful in other contexts:
* EventGen
* EventAccelerator
* StateMonitor

## EventGen
A standalone sample event generator that features a configurable state machine in order to create relatively
realistic looking data.  For example, a user might not normally be able to log into a mainframe without first logging onto a VPN.

Usage: `java stroom.analytics.demo.eventgen.EventGen <config file>` 

The config file is in yaml format.  Please see [example](event-gen/src/main/resources/ueba.yml)

It is also possible to define rare (special) kinds of state transition e.g. a user actually logging
onto the mainframe without logging onto the VPN first, and have the system separately record when
this occurs, in order to validate the results of downstream analytics, designed to identify these
via the event streams themselves.

Although state transitions are random, the likelyhood that they will occur can be configured and
daily schedules of activity can be defined, in order to provide a more naturalistic output.

Data is written to a number of CSV log files (one per output stream, of which multiple may be configured.

## EventAccelerator
An application that is designed to help test and demonstrate real-time stateful analytics without having to wait for 
the real amount of time to elapse.

Usage: `java stroom.analytics.demo.eventgen.EventAccelerator [seconds per accelerated hour] [delay from now in minutes] <file 1> ...[file n]` 

Input files should be CSV format with an ISO 8601 timestamp in the first field (suitable for processing `eventgen` output)
The output is created in an `eventAccelerator` directory that is created within the current directory.
The output directory contains a number of (or many) files, each containing the subset of the events in one of the
input files, where each file contains the events that relate to a single hour, although these are accelerated within 
the output, so that the recorded timestamp is a short amount of time after when the files were generated.

Output files are named `n.filename` where `n` is the number of hours after the earliest event in the set of all files processed. 

## StateMonitor
A Spark Structured Streaming stateful analytic.  It is designed to read events off a Kafka topic in JSON format. 
It is configurable via a yaml file and can be used to detect certain classes of unexpected state changes that could highlight various issues. 

Usage: please refer to the [launch script](demonstrator/bash/startStateMonitor.sh) for information about how to start within Spark.

# Building
Build is controlled by gradle.  The easiest way to build everything is using the provided script.
```shell script
cd demonstrator/bash
./buildall.sh
```
# Library Versions
N.B. The demonstrator requires [specific versions](docs/Versions.md) of certain components (e.g. Stroom, and Apache Spark).

# Further Information 
[Explore the docs...](docs/Demonstrator.md)
