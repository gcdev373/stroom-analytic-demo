# Importing and Enabling Stroom Content
The Stroom content provided within this repo is located in the archive
`demonstrator/stroom/StroomConfig.zip` . A second archive `demonstrator/stroom/StroomConfig-all.zip` contains this content
plus the standard content that it relies upon.

## Index Volume Groups

Stroom can be configured to automatically create an Index Volume Group on startup.
The standard name for this Index Volume Group is `Default Volume Group`. All Stroom instances that run
under docker, and have been installed with typical settings will have this available by default.

If you are using such a Stroom instance, you can proceed to import the Stroom content (see below).

If your Stroom instance does not have an Index Volume Group named `Default Volume Group`, or you do not wish
to use it for this demonstration, you will need to create one from within the Stroom UI.
The Volume Group should have at least one volume and specify your node name (default is `node1a`).

## Import Content

The Stroom UI should be used in order to import this content. Use the **Import** feature from the **Tools** menu in order to
import `StroomConfig-all.zip`.
Alternatively, the content pack `StromConfig.zip` can be imported, but in this case all the standard Stroom content, 
must also be installed, separately.

During import, ensure that the "Enable Processor Filters" check box is checked (ticked) to ensure that all processors are
started automatically.

##### Use Alternative Index Volume Group (Optional Step)
If you wish to use a different Index Volume Group to `Default Volume Group` you will need to use the Stroom UI
to modify the Settings of the newly imported indexes to use the alternative Index Volume Group.  The indexes are:
1. `Analytic Demonstrator/Annotations/Detections/Index/Detections Index`
1. `Analytic Demonstrator/Sample Index/Sample Index`

## Demonstrator Next Step
It is now possible to proceed with static and streaming data [analysis](analysis.md).