# Importing and Enabling Stroom Content
The Stroom content provided within this repo is located in the archive
`demonstrator/stroom/StroomConfig.zip` . A second archive `demonstrator/stroom/StroomConfig-all.zip` contains this content
plus the standard content that it relies upon.

## Import Content

The Stroom UI should be used in order to import this content. Use the **Import** feature from the **Tools** menu in order to
import either `StroomConfig.zip` or `StroomConfig-all.zip`.

## Enable Content 

In order to enable the imported Stroom content, it is necessary to create processor filters and volume groups via the Stroom UI.

#### Create Index Volume Groups.
1. Create a Index Volume Group called `Group1` containing a single volume with a Node name of `node1a` and a path of
`$HOME/stroom/analyticdemo/indexvols/group1`
1. Open the index `System/Analytic Demonstrator/Sample Index/Sample Index` using the Stroom UI. 
Ensure that the Volume Group `Group1` is selected
1. Open the index `System/Analytic Demonstrator/Analytic Output/Detections/Index/Detections Index` using the Stroom UI. 
Ensure that the Volume Group `Group1` is selected

If you would like to create the Index Volume Groups without using the Stroom UI, you can do it
 [directly via the database](databaseIndexVolumeCreation.md)

#### Create Processor Filters
Using the Stroom UI, processor filters should be created on the following pipelines.  This should be done manually in the Stroom UI.
For example, 1 below can be achived by creating a new processor on the pipeline`DEMO-EVENTS` 
with the filter set to `Type = 'Raw Events' AND (Feed Name = 'DEMO-MAINFRAME-EVENTS' OR Feed Name = 'DEMO-VPN-EVENTS')`:
1. **DEMO-EVENTS** to convert all `Raw Events` streams on feeds `DEMO-MAINFRAME-EVENTS` and `DEMO-VPN-EVENTS` into `event-logging` XML.
1. **Sample Index** to place all `Events` streams on feeds `DEMO-MAINFRAME-EVENTS` and `DEMO-VPN-EVENTS` into the index.
1. **Sample Topic** to place all `Events` streams on feeds `DEMO-MAINFRAME-EVENTS` and `DEMO-VPN-EVENTS` onto the topic.
1. **Detect Unusual Login Times** to run this single event / simple analysis against all `Events` streams on feeds `DEMO-MAINFRAME-EVENTS` and `DEMO-VPN-EVENTS`
1. **SAMPLE-DETECTIONS** to convert all `Raw Events` streams on feed `SAMPLE-DETECTIONS` into `detection` XML (type is `Detections`)
1. **Detections Index** to place all `Detections` streams on any feed into the index.
1. **SAMPLE-ALERTS** to create Annotations from all `Raw Events` streams on feed `SAMPLE-ALERTS`.

#### Enable Stream Processing
Enable Stream processing from the `Monitoring/Jobs` dialog of the Stroom UI (both globally and on `node1a`, assuming that this is a default, local installation)
