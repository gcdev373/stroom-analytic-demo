# Analytic Output as Detections
## Detections
The XML schema `detections:1` is provided within the Stroom content of this repo.  

It is possible for Stroom pipelines to create streams of type `Detections` that conform to this schema.

As with other kinds of XML, Detections can be indexed within Stroom and then searched using Stroom queries and the 
matching records displayed on dashboards. 

## Example Pipelines
The Stroom pipeline `SAMPLE-DETECTIONS` creates `Detections` streams from `Raw Events` streams, that are supplied in [CSV format](alertFormat.md)
to the Stroom feed `SAMPLE-DETECTIONS`

The Stroom pipeline `Detect Unusual Login Times` creates `Detections` streams from `Events` streams, but only when the events
match the specific conditions that it is designed to detect.  See [main page](SingleEventSimpleAnalysisWalkthrough.md) for details.

## Example Dashboard
The Stroom dashboard `Detections` is designed to search `Detections` streams that have been indexed by the pipeline 
`Detections` and thereby placed into the Stroom index `Detections Index`.

Running the search displays the `Detections` in a table.  The fields are extracted from the `Detection` documents by
the Search Extraction Pipeline `Detections INdex Search Extraction`.

One of the fields that is extracted by this pipeline is `LinkedEventExpression` which contains a JSON format Stroom query search expression
that can be used to search a Stroom index for all the referenced events, if there are any.  For more information about 
referenced events, please see the corresponding section of [this page](alertFormat.md).

Selecting a row of the table, results in a  