# VNC client container configuration

If you intend to support VNC connections to non-Windows systems (in additional RDP connections
for both Windows and non-Windows sessions) you will need to build VNC client containers using
the Dockerfile and components in this directory.

Note that like the RDP client stack, the VNC stack comes in a "default" and a "large" flavor.  
The runvncserver.default and writexclients.default files here differ from the runvncserver.large
and writexclients.large files only in that the *.large files run the appropriate display code
with a larger-than-default geometry.  Copy the *.default files onto "writexclients" and 
"runvncserver" in this directory to build a container that uses the default screen sizes, or
the *.large files to build a container that uses the larger screen sizes.
