# Event Normalisation
Stroom is most effective when it is used to process Streams of events in XML format.
Processing can take advantage of the power and flexibility of XLST in order to transform data into other formats and
perform other operations.

The XML format [`event-logging`](https://github.com/gchq/event-logging-schema) is recommended, and it is used within this
demonstration.

The pipeline **Analytic Demonstrator/Sample Feeds/DEMO-EVENTS** is used to process CSV `EventGen` data into `event-logging` XML docs.

In order for it to pick up and process incoming Streams, it is necessary to add a Processor filter to the pipeline.

The processor filter should be configured to select:
 
 *All Streams of type `Raw Events` where feed name is `DEMO-MAINFRAME-EVENTS` or `DEMO-VPN-EVENTS`*
 
 Once it is enabled, all Raw Events Streams on either of the two feeds will automatically be fed through the normalisation pipeline
 which will result in a new stream of type Event that will also be associated with the same feed.
 
This new stream will be picked up by processor filters on other pipelines that [index](indexing.md) the events and
[place them onto a Kafka topic](kafkaproducer.md) in order to enable further analysis.