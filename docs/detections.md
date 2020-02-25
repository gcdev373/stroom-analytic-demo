# Analytic Output as Detections
The XML schema `detections:1` is provided within the Stroom content of this repo.  

It is possible for Stroom pipelines to create streams of type `Detections` that conform to this schema.

The Stroom pipeline `SAMPLE-DETECTIONS` creates `Detections` streams from `Raw Events` streams, that are supplied in [CSV format](alertFormat.md)
to the Stroom feed `SAMPLE-DETECTIONS`

The Stroom pipeline `Detect Unusual Login Times` creates `Detections` streams from `Events` streams, but only when the events
match the specific conditions that it is designed to detect.  See [main page](SingleEventSimpleAnalysisWalkthrough.md) for details.