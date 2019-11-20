# stroom-analytic-demo
Stroom has powerful capabilities for collection, 
normalisation and storage of data.

However, in order to analyse this data, it is necessary to
integrate Stroom with an external anaytic framework.

This repo presents just one possible approach.

# EventGen
This repo includes a standalone Sample event generator that features a configurable state machine in order to create relatively
realistic looking data.  For example, a user might not normally be able to log into a mainframe without first logging onto a VPN.

It is also possible to define rare (special) kinds of state transition e.g. a user actually logging
onto the mainframe without logging onto the VPN first, and have the system separately record when
this occurs, in order to validate the results of downstream analytics, designed to identify these
via the event streams themselves.

Although state transitions are random, the likelyhood that they will occur can be configured and
daily schedules of activity can be defined, in order to provide a more naturalistic output.

Data is written to a number of log files (one per output stream, of which multiple may be configured.

# Further Information
[Explore the docs...](docs/Demonstrator.md)
