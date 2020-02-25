# Single Event Analysis with Simple Analytics
The simplest forms of streaming analytic are those that identify specific kinds of event by only considering the
properties of a single event, and possibly static reference data.

Stroom is very capable of supporting such analytics and an example is provided within this repo.

## Design
Such analytics are implemented by a pipeline that contains an XSLT filter element that performs the actual analysis.

There are various ways to design such an analytic:
1. Use identity filter to copy matching events only, and place onto another feed, or the same feed with a different stream type.
1. Create `annotation` XML documents for matching events and then use the `AnnotationWriter` pipeline element to create annotations directly.
1. Create `detection` XML documents for matching events and place onto another feed, or the same feed with a different stream type.

The supplied example uses the third of these approaches.  By writing `detection` documents as the `Detections`, ensures that
processor filter on `Detections Index` that matches `Type = 'Detections'` and therefore all feeds, will index the output of the pipeline.

## Implementation
The example analytic is `Analytic Demonstrator/Sample Analytics/Single Event Simple Analysis/Detect Unusual Login Times`
it matches any login event that is not within certain defined office hours.

N.B. This analytic is is intended to be illustrative of the approach rather than useful in its own right.

## Running The Demonstration
The following steps will walkthrough the demonstration

### 1. Import and enable Stroom Content
Ensure that the Stroom Content is [enabled](enableStroomContent.md).

### 2. Generate Events
The Java application `eventgen` can be used to generate events.
A suitable set of events can be generated using the following command from the repo root:
```shell script
demonstrator/bash/runEventGen.sh
```

Events are generated into `tmp/eventgen` and are contained within two separate files, one for each feed.

### 3. Send Events to Stroom
The files can be sent to the Stroom datafeed API with the following commands:

```shell script
cd tmp/eventgen
../../demonstrator/bash/sendToStroom.sh
```

### 4. Observe Stroom Processing
It is possible to observe Stroom processing the data, either by inspecting log files or via the UI.

The sequence of processing is as follows:
1. CSV files are sent to Stroom feed `DEMO-VPN-EVENTS` and `DEMO-MAINFRAME-EVENTS`.
1. Stroom stores the CSV data as new `Raw Events' streams.
1. The processor filter on the Stroom pipeline `DEMO-EVENTS` matches the new `Raw Events` stream and begin to process them.
1. The `Raw Events` streams are processed into `Events` streams as the XSLT `DEMO-EVENTS` creates `event-logging` XML documents.
1. The newly created `Events` streams are stored on the feeds that held the `Raw Events` streams from which they were derived.
1. The processor filter on the analytic pipeline `Detect Unusual Login Times` matches the new `Events` streams and begins to process them.
1. The `Events` streams are processed into `Detections` streams as the XSLT `Detect Unusual Login Times` creates `detection` XML documents.
1. The newly created `Detections` streams are stored on the feeds that held the `Events` streams from which they were derived.
1. The processor filter on the indexing pipeline `Detections Index` matches the new `Detections` streams and begins to process them.
1. The XSLT `Detections Index` processes`Detections` streams into `records` that are used by the `IndexingFilter` pipeline to index the `Detections` streams.

### 4. Inspect Analytic Output
The dashboard  `Detections` can be used to show all the analytic output.

A filter can be added in order to narrow the query and view only the detections that were created by this analytic.

For further information about viewing detections, see this [page](detections.md)