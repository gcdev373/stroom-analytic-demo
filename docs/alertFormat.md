# Alert Format
This demonstrator provides analytic applications that execute separately to Stroom, and are designed to detect certain conditions.

A standard output format is used for these analytics, in order to minimise the amount of Stroom content needed. 

## Processing the output within Stroom
The output of these analytics is written into a standard CSV format that can be processed by Stroom in either or both of the following ways:
1. Correctly formatted output received on the feed `SAMPLE-ALERTS` is processed ino Annotations
1. Correctly formatted output received on the feed `SAMPLE-DETECTIONS` is processed into Detections.

For more information about these approaches, see the [main page](analyticOutput.md).

## Format
Standard CSV format with no header line.

Each line the fields are as follows:
1. Date/timestamp - ISO8601 format
1. Headline / alert name
1. Description 
1. Details

There then follow any number of pairs of fields, the first of each pair is a an arbitrary property name, and the second its value.
If the name is the literal `eventref` then the value is a referenced event (see below).

## Referenced Events
It is important to be able to identify which events within Stroom led to the detection being made.  This can be achieved
by providing one or more `eventref` properties, each with one or more  strings of the format `StreamId:EventId`.

This allows triage to be conducted, as it is usually necessary to refer back to the original event or events when trying
to understand the nature of the detection. 