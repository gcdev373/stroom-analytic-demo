# Raw Data Within Stroom
A batch of data within Stroom is known as a Stream.  Feeds are normally configured in order to assign the Stream Type `Raw Events`
to all batches that are received via the [data feed API](post.md). 

Streams are retained for the length of time determined by a retention policy can be browsed from the UI and / or processed.

One of the benefits of Stroom over similar products is that individual batches of data can easily be found and processed as many times as necessary.
Raw data can be retained in order to allow it to be inspected or reprocessed should a change to normalisation be required in the future.

Stroom pipelines are created in order to actually perform the processing, these operate on an input Stream and either create an output stream
or perform some other function, e.g. write to Kafka or an external file.  

This repo contains the following Stroom pipelines:

* **Analytic Demonstrator/Sample Feeds/DEMO-EVENTS** is used to process CSV `EventGen` data into `event-logging` XML docs.
* **Analytic Demonstrator/SampleIndex/Sample Index** is used to populate the `Sample Index` with `EventGen` data.
* **Analytic Demonstrator/Sample Topic/Sample Topic** is used to to create Kafka events representing `EventGen` data. 

These are each described in later sections of this guide.

The first of these pipelines is used to [normalise the event data](normalisation.md).