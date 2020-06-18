#
# From the demo sshd docker from docker.io
#

FROM carte018/vncstack

MAINTAINER "rob@duke.edu"

#
##writexclients is a shell script that handles priming the .Xclients file in he home directory for VNC startup
#
ADD writexclients /tmp/writexclients
#
##runvncserver is a shell script wrapper that handles executing the vncserver
#
ADD runvncserver /tmp/runvncserver
#
