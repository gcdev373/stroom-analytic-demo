# Importing and Enabling Stroom Content
The Stroom content provided within this repo is located in the archive
`demonstrator/stroom/StroomConfig.zip` . A second archive `demonstrator/stroom/StroomConfig-all.zip` contains this content
plus the standard content that it relies upon.

## Import Content

The Stroom UI should be used in order to import this content. Use the **Import** feature from the **Tools** menu in order to
import `StroomConfig-all.zip`.
Alternatively, the content pack `StromConfig.zip` can be imported, but in this case all the standard Stroom content, 
must also be installed, separately.

During import, ensure that the "Enable Processor Filters" check box is checked (ticked) to ensure that all processors are
started automatically.

## Enable Content 

In order to enable the imported Stroom content, it is necessary to create processor filters and volume groups via the Stroom UI.

#### Create Index Volume Groups.
1. Create a Index Volume Group called `Group1` containing a single volume with a Node name of `node1a` and a path of
`$HOME/stroom/analyticdemo/indexvols/group1` (or specify an alternative location).
1. Open the index `System/Analytic Demonstrator/Sample Index/Sample Index` using the Stroom UI. 
Ensure that the Volume Group `Group1` is selected
1. Open the index `System/Analytic Demonstrator/Analytic Output/Detections/Index/Detections Index` using the Stroom UI. 
Ensure that the Volume Group `Group1` is selected

If you would like to create the Index Volume Groups without using the Stroom UI, you can do it
 [directly via the database](databaseIndexVolumeCreation.md)

#### Enable Stream Processing
Enable Stream processing from the `Monitoring/Jobs` dialog of the Stroom UI (both globally and on `node1a`, assuming that this is a default, local installation)

## Demonstrator Next Step
It is now possible to proceed with static and streaming data [analysis](analysis.md).