# Analytic Output as Annotations
## Annotations
A feature of core Stroom since version`6.1`, annotations enable users to associate comments with events, and can
support a basic event triage workflow.  Annotations are stored in the Stroom database and have a number of version
controlled attributes.  They also have a set of linked events, which represent the set of events for which this
Annotation applies.  N.B. The relationship between Annotation and Event is many-to-many.

## Annotation Creation

Stroom v7.0 introduced  a new pipeline element `AnnotationWriter` that allows
Annotations to be created automatically by Stroom pipelines, rather than having to be created manually, within the Stroom UI.  
This means that Annotations can be used to represent analytic output / alerts.

This repo contains the XML schema `annotations:1`, which defines the XML schema that the `AnnotatioWriter` pipeline element
requires.

## Example

The Stroom pipeline `SAMPLE-ALERTS` uses the similarly named XSLT in order to convert `Raw Events` streams in the
[analytic output CSV format](alertFormat.md) into `annotations` XML documents that are finally passed through a
`AnnotationWriter` pipeline element, that then creates Annotation records within the Stroom database.

## Viewing Annotations
Annotations can be viewed using a Stroom dashboard that uses the Data Source `Annotations`.  A simple example dashboard,
`Annotations` is provided within the repo.

Running the query will populate the table with the basic information for all matched Annotations.

Every field is rendered within the Stroom UI as a hyperlink.  Clicking this link brings up an Annotation editor dialog,
populated with the currently selected Annotation.

This is achieved by using a dashboard expression that calls the expression function `annotation`.  For example:
```ex
annotation(${annotation:Subject}, ${annotation:Id}, ${StreamId}, ${EventId})
```
See Stroom documentation for further information.