# stroom-analytic-demo
Stroom has powerful capabilities for collection, 
normalisation and storage of data.

However, in order to analyse this data, it is necessary to
integrate Stroom with an external anaytic framework.

This repo presents just one possible approach.

# EventGen
This repo includes a standalone Sample event generator that features a configurable state machine in order to create relatively
realistic looking data.  For example, a user might not normally be able to log into a mainframe without first logging onto a VPN.

It is also possible to define rare (special) kinds of state transition e.g. a user actually logging
onto the mainframe without logging onto the VPN first, and have the system separately record when
this occurs, in order to validate the results of downstream analytics, designed to identify these
via the event streams themselves.

Although state transitions are random, the likelyhood that they will occur can be configured and
daily schedules of activity can be defined, in order to provide a more naturalistic output.

Data is written to a number of log files (one per output stream, of which multiple may be configured.

# EventAccelerator
An application that is designed to help test and demonstrate real-time stateful analytics without having to wait for 
the real amount of time to elapse.

Usage: `java java stroom.analytics.demo.eventgen.EventAccelerator <file1> ...[filen]` 
Input files should be CSV format with an ISO 8601 timestamp in the first field (suitable for processing `eventgen` output)
The output is created in an `eventAccelerator` directory that is created within the current directory.
The output directory contains a number of (or many) files, each containing the subset of the events in one of the
input files, where each file contains the events that relate to a single hour, although these are accelerated within 
the output, so that the recorded timestamp is a short amount of time after when the files were generated.

Output files are named `n.filename` where `n` is the number of hours after the earliest event in the set of all files processed. 

# StateMonitor
A Spark Structured Streaming stateful analytic.  It is designed to read events off a Kafka topic in JSON format. 
It is configurable and can be used to detect certain classes of unexpected state changes that could highlight various issues. 


# Further Information
[Explore the docs...](docs/Demonstrator.md)
