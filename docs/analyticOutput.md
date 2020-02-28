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

## Discussion
Which option for analytic output (`Detections` or annotations) is preferred, is a design decision.

### Functionality

At the current time, both approaches can be used to achieve broadly similar results - storage of alert details to enable
triage together with their linked events (if any).  

However, annotations are a relatively new feature of Stroom, functionality and support for them is gradually increasing. 
In the future functionality might differ significantly, and any decision might need to be revisited.

### Scalability

Annotations are stored in the Stroom RDBMS, and are therefore inherently far less scalable than Detections.

It is usually impossible to absolutely guarantee that an analytic will not encounter some unexpected input that might cause it to
fire far more often than expected. Where annotations are automatically created from alerts, there will always be a risk that
the database could be adversely affected by such an unexpected increase in insert rate.

### Security Model
Annotations have a different security model to that used for other data within Stroom.  It is not granular, and users
that have access to annotations must be granted the `Data Browsing` role.

Detections are normal Stroom data, and therefore benefit from all the granular controls that can be placed onto Stroom data.

It is possible to create different sets of Detections on different feeds, and control which users see which Detections
by their role.  

### Discussion

At this time, it is suggested that Detections should normally be favoured over annotations as the main way to handle
analytic output / alerts within Stroom.

This restricts annotations to only those that are created manually by users, and eliminates the risk to the RDMBS.

Even in deployments where all annotations are manually created, there might be situations where the pipeline element 
`AnnotationWriter` could be used to support this manual creation of annotations.   For exmample, the `Pipeline` tool 
of the query toolbar from a dashboard could be used to prepopulating the new annotations with information derived 
from Detections in a standard way.  Such an approach would reduce user effort and increase consistency. 