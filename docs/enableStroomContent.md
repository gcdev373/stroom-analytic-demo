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

N.B. The analytic demonstrator creates two indexes that use the index group called "Default Volume Group" that is usually
available with a clean installation of Stroom v7. If you wish to use an alternative volume group, then the following
indexes will need to be modified in order to select the volume group that you wish to use:
* Sample Index
* Detections Index

#### Enable Stream Processing
Enable Stream processing from the `Monitoring/Jobs` dialog of the Stroom UI (both globally and on `node1a`, assuming that this is a default, local installation)

## Demonstrator Next Step
It is now possible to proceed with static and streaming data [analysis](analysis.md).