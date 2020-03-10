# Indexed Data with Stroom
Analytic development can be faster and more successful if interactive searching over the data is supported in particular in the
initial, exploratory stages.

The approach described below, allows interactive searches to be iteratively developed into streaming analytics.

## Stroom Indexing
Stroom uses Apache Lucene in order to index data.  The pipeline `Sample Index` illustrates how this is accomplished.

The pipeline filter element `Indexing Filter` creates indexes based on fields in `records` XML format.  It 
converts `events-logging` normalized events into `records` within an `XSLT filter` element, before 
passing these into the `Indexing Filter`.

The pipeline`Sample Index` updates the `Sample Index` index, which can be used as a data source for interactive and API
searches.

## Interactive Searches from Stroom
Once indexed, it is possible to search the `Sample Index` as a datasource.  The dashboard `VPN Access` is a simple example of this searching.

It is possible to add filters, sort results and further process the results of interactive searches from within the Stroom UI.

## Search Extraction
In order to minimise their size on disk, the number of fields held for each record, within the index itself is minimised.  Very often,
only `StreamId` and `EventId` are recorded, in order that the original event can be located and the fields that are displayed within
the table, or returned via the Stroom API are created by a Search Extraction Pipeline.

Every dashboard can specify a search extraction pipeline to use.  Calls to the Stroom index API can similarly specify a search
extraction pipeline.

Search Extraction Pipelines are very similar to indexing pipelines, in that they create fields as `records` XML, and
these are converted from the format that the data that was indexed is held in, e.g. `events-logging` format XML.  It is possible
to use the same XSLT translation from `events-logging` XML into `records` XML within both an indexing pipeline and a
search extraction pipeline.  Although this would naturally provide the same set of fields within the extraction as that which
are indexed, this behaviour can be modified via the index definition and/or conditional logic within the XSLT.
See the pipeline `Sample Index Search Extraction` for an example.

Search Extraction pipelines can create entirely different fields to those defined in the index.  For example, the standard
Search Extraction pipeline `JSON Extraction` creates a JSON representation of `event-logging` XML format `Event` elements.
This is useful because the same kinds of data can also be [published onto a Kafka topic in this format](kafkaproducer.md),
simplifying the analytic development process. 

## REST API Searches from Stroom
Indexed data can be searched via the Stroom REST API.  The URL for a standalone, demonstration Stroom instance is
 `http://localhost:8080/api/stroom-index/v2/search`.  

It is possible to export the query conditions from an interactive query in Stroom, and use these within a call to the
search API.  This can simplify the development process when creating standalone analytics that use the Stroom API directly.
Refer to [Stroom Documentation](https://gchq.github.io/stroom-docs/user-guide/api/query-api.html)
for details.

## Searching via Apache Spark
An alternative to calling the Stroom search API REST service directly, is to use the `stroom-spark-datasource` for Apache
Spark.  This provides a facade over the Stroom search API, exposing Spark's `DataSource v2` interface, and thus enabling
Stroom indexed data to be easily accessed from a Spark application.

This can be a very powerful way to develop analytics.  An [example of this approach, using Jupyter Notebook](jupyterAnalysis.md)
in order to process `event-logging` XML data that is converted into JSON format during Search Extraction is provided within this repository.

