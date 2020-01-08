# Data Analysis using Jupyter Notebook
# Introduction
It is possible to perform Simple, Single Event Analysis directly within Stroom.  
However, Stroom is not currently ideal for more advanced single event analysis 
(e.g. those requiring advanced statistical techniques), and it makes sense to utilise an external framework for meeting 
such requirements.

This repo contains a number of Jupyter Notebooks that illustrate one way to perform Complex, Single Event Analysis.

# Objective
This analytic is an attempt to detect unexpectedly high numbers of failed authentication attempts using a machine learning approach.

# Lifecycle
Although only intended to demonstrate a single analytic, there are three notebooks that together demonstrate the following lifecycle:
1. Data Exploration
1. ML Training
1. Streaming analysis

# Description
The `stroom-spark-datasource` is used in conjunction with a Stroom extraction pipeline that creates JSON in order to 
unify the format of events returned from queries (stages 1 and 2) with those from Kafka (stage 3).

This allows the same python code that is developed to perform data exploration to be developed further for feature engineering.

And then that feature engineering code can be reused within a streaming context with only minimal changes.

# Running
As the notebooks query Stroom directly, it is necessary for Stroom to be running with the test data correctly indexed prior to
running the notebooks.

Spark v2.4.3 should be installed and `$PATH` set to include `$SPARK_HOME/bin`.

The Jupyter server can be started with the script `demonstrator/bash/startJuptyer.sh`.  

The notebooks should then be loaded via the browser.  They are contained in the directory `
demonstrator/analytics/jupyter/singleEventComplexAnalysis`.