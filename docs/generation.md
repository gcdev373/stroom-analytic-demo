# Event Generation
Computer systems and applications typically write events to log files or send via a network protocol to a consuming service.

Stroom requires a batch of events.  A log file can be rolled periodically, or a script can be run in order to extract
aggregated events from an event consuming service.

This will normally result in a text format.  Any text format is compatible with Stroom.  However, if a binary format is
used, then it should be converted into text with a suitable utility application prior to [sending to Stroom](post.md).

This demonstrator includes `EventGen`, a Java application that creates the events that will be used.

## Running EventGen
EventGen can be run using the configuration designed for this demo using the script `demostrator/bash/runEventGen.sh`.

The output is written to 'tmp/eventgen' from the repo root, the following CSV format text files are created:
* DEMO-MAINFRAME-EVENTS.txt - contains simulated mainframe user events.
* DEMO-VPN-EVENTS.txt - contains simulated vpn user events.
* special.out - special events (described below)

The stochastic process that created the text files is controlled by the YAML format configuration file.
The configuration file used within this demonstration allows users to move through a number of states as they log onto
a VPN and then possibly onto a mainframe.  The simulation also includes a number of rarer (presumably undesirable) state changes
that relate to a user's mainframe credentials being used without them first logging onto the VPN.

These special situations are included in order to exercise analytics that may be designed to identify these.
In order to confirm that such analytics are performing as expected, these rarer state changes are recorded in the text file
`special.out`

## Rare State Changes (Special Events)
When a user logs into the mainframe, they are normally already logged into the VPN.  
However this simulation allows the user to log into the mainframe without previously logging into the VPN either
with or without a larger than average number of failed authentications (a simulated brute force attack, from a partial
password).

This repo includes a [demonstration of using machine learning features within Apache Spark](JupyterAnalysis.md) to identify spikes of authentication failures.

It also includes the application `StateMonitor` that demonstrates [stateful processing within Spark Structured Streaming](MultipleEventAnalysisWalkthrough.md)
and is capable identify unexpected state changes, directly.


 
 