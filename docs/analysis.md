# Analyis
Although Stroom is most typically used for near-real-time analysis of data (streaming analysis), such streaming analytics
can be difficult to develop and test.

It is desirable that analytics that are developed interactively, using searches against the index can later be
converted into streaming analytics.  In order to facilitate this process, the fields of the index are duplicated
as Kafka headers.  `Sample Topic` XSLT contains the directive `xsl:include("Sample Index")` in order to duplicate 
this logic whilst preventing duplication of XSLT code.

## Static Data Analysis
Stroom dashboards are a useful tool for analysis of indexed data.  However, it might be desirable to use third-party
data analysis or visualisation libraries, e.g. for machine learning.

An example of such analysis that utilises Jupyter Notebook is presented [here](JupyterAnalysis.md)

## Streaming Data Analytics
Different approaches are appropriate for different classes of problem.

### Type One: Single Event / Simple Analysis
The simplest kind of analytic can be achieved effectively within Stroom itself.  See [walkthrough](SingleEventSimpleAnalysisWalkthrough.md).

### Type Two: Single Event / Complex Analysis
More complex analytics can be achieved externally and driven via Kafka.  See [walkthrough](SingleEventComplexAnalysisWalkthrough.md).

### Type Three: Multiple Event Analysis
Stateful processing can also be driven via Kafka.  See [walkthrough](MultipleEventAnalysisWalkthrough.md)

## Output From Analytics
It is often desriable that all analytics feed into the same triage process within Stroom itself.  This repo illustrates two different ways to achieve this:  
1. Detections - creation of special events that can be indexed in order to facilitate triage.
1. Annotations - direct creation of annotations, that are thereby incorporated into the standard Stroom workflow.

### Detections
Further information about this approach for handling analytic output can be found [here](detections.md).

### Annotations
Further information about this approach for handling analytic output can be found [here](annotations.md).


