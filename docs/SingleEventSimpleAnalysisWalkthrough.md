# Single Event Simple Analysis
The simplest forms of streaming analytic are those that identify specific kinds of event by only considering the
properties of a single event, and possibly static reference data.

Stroom is very capable of supporting such analytics and an example is provided within this repo.

## Design
Such analytics are implemented by a pipeline that contains an XSLT filter element that performs the actual analysis.

There are various ways to design such an analytic:
1. Use identity filter to copy matching events only, and place onto another feed, or the same feed with a different stream type.
1. Create `annotation` XML documents for matching events and then use the `AnnotationWriter` pipeline element to create annotations directly.
1. Create `detection` XML documents for matching events and place onto another feed, or the same feed with a different stream type.

The supplied example uses the third of these approaches.  By writing `detection` documents as the `Detections`, ensures that
processor filter on `Detections Index` that matches `Type = 'Detections'` and therefore all feeds, will index the output of the pipeline.

## Implementation
The example analytic is `Analytic Demonstrator/Sample Analytics/Single Event Simple Analysis/Detect Unusual Login Times`
it matches any login event that is not within certain defined office hours.

N.B. This analytic is is intended to be illustrative of the approach rather than useful in its own right.

## Running Demonstration

