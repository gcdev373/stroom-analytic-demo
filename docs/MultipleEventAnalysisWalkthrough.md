# Multiple Event Analysis - Walkthrough
How to use the the various tools within this repo to provide an end-to-end demonstration of stroom analytics using Spark Structured Streaming and Kafka

# 1. Start Stroom
Please start Stroom, see Stroom documentation for instructions
The following Stroom services (docker images) are also needed:
* stroom ui (`stroom-ui`)
* stroom auth (`stroom-auth-service`)
* stroom database (`stroom-all-dbs`)
* nginx (`nginx`)
* kafka (`kafka`)
* zookeeper (`zookeeper`)

# 2. Import Stroom Content
Using the Stroom UI, import `demonstrator/stroom/StroomConfig-all.zip`

# 3. Enable Stroom Content
1. Create a Index Volume Group called `Group1` containing a single volume with a Node name of `node1a` and a path of
`/tmp/stroom/analyticdemo/indexvols/group1`

1. Open the index `System/Analytic Demonstrator/Sample Index/Sample Index` using the Stroom UI. 
Ensure that the Volume Group `Group1` is selected

1. Create the following Processor Filters using the Stroom UI:
    * All Streams of type `Raw Events` where feed name is `DEMO-MAINFRAME-EVENTS` or `DEMO-VPN-EVENTS` on pipeline
    `System/Analytic Demonstrator/Sample Feeds/Demo Events`
    * All Streams of type `Events` where feed name is `DEMO-MAINFRAME-EVENTS` or `DEMO-VPN-EVENTS` on pipeline
     `System/Analytic Demonstrator/Sample Index/Sample Index`
    * All Streams of type `Events` where feed name is `DEMO-MAINFRAME-EVENTS` or `DEMO-VPN-EVENTS` on pipeline
         `System/Analytic Demonstrator/Sample Topic/Sample Topic`

1. Enable all processors and processor filters that were created above.  This is normally possible via the UI but due to
a bug in the version of Stroom used within this demo, it is necessary to do this directly within the database:
    * The database can be accessed by using the following command `source stroom-resources/.aliases;stroomdb` 
    (requires clone of `stroom-resources` repo)
    * Type the following commands: `update processor set enabled = true;` followed by 
    `update processor_filter set enabled = true;`

1. Enable Stream processing from the `Monitoring/Jobs` dialog of the Stroom UI (both globally and on `node1a`)

# 4. Generate Events
The Java application `eventgen` can be used to generate events.
A suitable set of events can be generated using the following command from the repo root directory:

`java -cp event-gen/build/libs/event-gen-all.jar stroom.analytics.demo.eventgen.EventGen ueba.yml`

The events are generated into `/tmp/eventgen` and are contained within two separate files, one for each feed.

An additional file `special.out` records atypical events that might be significant and / or detected using an analytic.

# 5. Start StateMonitor
The Spark Streaming application `EventMonitor` should now be started.  It will poll Kafka for events in JSON format.

Prerequisites: It is necessary to install Spark `v2.4.3` and set `$SPARK_HOME` to this distribution.

A script is provided to start the application.  You should start a Java 8 shell and `cd` into `demonstrator/bash`.
Then type `./startStateMonitor.sh`

# 6. Provide Input
A convenience script is provided that carries out the following two steps (**6A** and **6B**).
```shell script
cd demonstrator/bash
./accelerateEventsAndSendToStroom.sh
```
## 6A. Accelerate Events
The generated events are representative of real user activity over a period of 15 days.
In order to avoid having the test take 15+ days to complete, it is therefore necessary to compress the time periods involved.

This can be achieved with another java application, which should be run from where the events have been generated 
(i.e. `/tmp/eventgen`).  The following command will accelerate the events such that one hour lasts only 60 seconds, and the
first event is three minutes from now.
 
`java -cp event-gen/build/libs/event-gen-all.jar stroom.analytics.demo.eventgen.EventAccelerator 60 3 DEMO-MAINFRAME-EVENTS.txt DEMO-VPN-EVENTS.txt`

The output is placed into a directory `eventAccelerator` off the working directory (i.e. `/tmp/eventgen/eventAccelerator`)

Each file contains data for a specific feed that occurred within a particular period of time 
- each batch contains events from the equivalent of a duration of 1 hour (at original rate, prior to acceleration)

## 6B. Feed Events to Stroom
A script is provided that feeds the batches of events to Stroom at the required rate.
                     
This must be run from the directory containing the accelerated events (e.g. `/tmp/eventgen/eventAccelerator`)

`bash/sendAcceleratedEventsToStroom.sh 60`

# 7. Allow Demonstration Time To Run
This end-to-end demonstration requires hundreds of batches of data to be uploaded individually to Stroom.
These are normalised into `event-logging` XML and stored within Stroom.
 
They are then converted into JSON as they are placed onto the Kafka topic.

Spark reads data from Kafka and `StateMonitor` generates state whilst looking for unexpected state transitions.
When these occur, they are reported into a file and will be alerted to Stroom.
At 60 seconds per hour, it takes a total of 6 hours to complete this entire test.

Alternatively, follow these instructions if you would like to run a quicker test.

# 8. Assess results 
The output from `StateMonitor` is recorded in a file that is specified as the application starts.
This should be inspected and compared against `special.out` to determine whether all special events have been identified
correctly.