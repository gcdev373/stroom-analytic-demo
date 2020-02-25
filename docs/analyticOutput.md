# Analytic Output
It is desirable that all analytics, whether they are implemented as Stroom pipelines, or a separate technology external to 
Stroom are configured in order to produce output (alerts, etc) that can be triaged in a single, unified manner.

Two options for such triage are presented:
1. As indexed `Detections` streams
1. As Stroom annotations.

## Detections
When the [Stroom content](enableStroomContent.md) is enabled, it is possible to create `Detections` within Stroom by
sending [correctly formatted](alertFormat.md) CSV format data to the feed `SAMPLE-DETECTIONS`.

It is then possible to view the resulting `Detections` by opening the dashboard `Detections`.

Further information about `Detections` can be found [here](detections.md). 

## Annotations
When the [Stroom content](enableStroomContent.md) is enabled, it is possible to create Stroom Annotations for the alerts by
sending [correctly formatted](alertFormat.md) CSV format data to the feed `SAMPLE-ALERTS`.

It is then possible to view the resulting Annotations by opening the dashboard `Annotations`.

Further information about Annotations can be found [here](annotations.md).
