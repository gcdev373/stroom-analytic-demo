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

### Viewing Linked Events

One of the fields that is extracted by this pipeline is `LinkedEventExpression` which contains a JSON format Stroom query search expression
that can be used to search a Stroom index for all the referenced events, if there are any.  For more information about 
referenced events, please see the corresponding section of [this page](alertFormat.md).  

In order to prevent Stroom converting or otherwise escaping the JSON expression, all double quotes `"` have been replacd
with the logical negation symbol `¬`

The "Linked Events" field contains a hyperlink that opens a new dashboard (also called `Linked Events`) 
that immediately runs a query for all the linked events associated with this Detection.  

This is achieved using the following Expression (note use of `replace` to change the logical negation symbols back into
double quotes:
```ex
dashboard('Show','19f1b1d9-35e5-4ebb-99c9-6e1c1083785b','expressionJson=' + replace(${LinkedEventExpression},'¬','&#34;'))
```
The restored JSON expression string is assigned to the parameter `expressionJson`.  The parameter `expressionJson` is 
a special parameter that is recognised by Stroom as containing additional expression terms.  These additional
terms are added (logically ANDed) to any existing terms that the dashboard might (i.e. those that it had when saved).

Note. The JSON format for conditions is the same as the format used by the Stroom Search API, and it is easy to persist
conditions in this format from within the Stroom UI, using the `Download Query` option from the query toolbar within
the `Query` pane of a dashboard.

### Custom Text Pane
Selecting a row of the table, results in the text pane below the table, titled "Selected Detection" being updated with
details from the row's Detection.

The Search Extraction Pipeline `Example Detection Display` is used to create HTML DOM elements from a `detection` XML
document.  
 