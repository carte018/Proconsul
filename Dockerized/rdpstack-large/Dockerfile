#
# From the demo sshd docker from docker.io
#

FROM carte018/rdpstack

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

# This sets the default time zone to EST - YMMV
RUN rm /etc/localtime
RUN ln -s /usr/share/zoneinfo/US/Eastern /etc/localtime
# And fixes a bug in the gcc build that causes locales to be busted
RUN localedef -i en_US -f UTF-8 en_US.UTF-8
RUN touch /tmp/localdefed
VOLUME "/var/spool/docker"
CMD ["/usr/bin/supervisord","-c","/etc/supervisord.conf"]

