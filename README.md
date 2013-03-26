# FennecADT

This plugin enables Fennec (Firefox for Android) to be built using Eclipse.

## Installation
Unfortunately, there are currently no build scripts that automate the creation
of FennecADT's jar, so you'll need to manually package the plugin using
Eclipse:
* Import the FennecADT project into Eclipse
  * File > Import... > Existing Projects into Workspace
* Right click the project and choose Export > Deployable plug-ins and fragments
* Save the plugin to your \<eclipse-application-dir\>/dropins directory
* Restart Eclipse
