# Providing Data To Stroom
Stroom accepts event data via its data feed API.  HTTP or HTTPS is used in order to `POST` batches of data to the correct URL.

It is possible to configure authentication for the datafeed API, but this is not required for demonstration purposes.

The standard dataflow within production is via Stroom Proxy.  This aggregates batches of data and therefore reduces the load
on the downstream stroom.  THe standard path (for Stroom v7) is `stroom/noauth/datafeed` so for a stroom running on `localhost`, 
the following URL can be used `https://localhost/stroom/noauth/datafeed`.  

In a development environment where scaling is not such an issue, it is not necessary to go via Stroom Proxy and the
batch of data can be supplied to Stroom directly.  This has the added advantage of avoiding the additional latency caused
by the aggregation process within Stroom Proxy.  The direct URL is slightly different, and is not capable of HTTPS as it is
not fronted by NGIX, but merely provided by DropWizard directly: `http://localhost:8080/stroom/noauth/datafeed`

Any HTTP/S client can be used for this purpose.  The syntax for `curl` is shown below.

```shell script
curl -k --data-binary @thebatch.txt "http://localhost:8080/stroom/noauth/datafeed" -H "Feed:MY-EVENTS"
``` 

The above command will post a single batch of data (contained in the file `thebatch.txt`).
 
All data received into Stroom is associated with a Feed.  These are configured using the Stroom UI and then can be
exported for backup and sharing with other Stroom instances.  The above command sends the data to a previously created Stroom feed
called `MY-EVENTS`.

This demonstrator involves two feeds:
* DEMO-VPN-EVENTS
* DEMO-MAINFRAME-EVENTS

These are designed to receive the CSV data that is output from `eventGen` when it is executed using the [configuration file](event-gen/src/main/resources/ueba.yml) 

An example script for `POST`ing data to Stroom is [here](../demonstrator/bash/sendToStroom.sh).

[Raw data is stored within Stroom](rawstream.md) and may be processed in a variety of ways.