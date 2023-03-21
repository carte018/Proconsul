#
# From the demo sshd docker from docker.io
#

FROM carte018/rdpstack

MAINTAINER "rob@duke.edu"

# This sets the default time zone to EST - YMMV
RUN rm /etc/localtime
RUN ln -s /usr/share/zoneinfo/US/Eastern /etc/localtime
# And fixes a bug in the gcc build that causes locales to be busted
RUN localedef -i en_US -f UTF-8 en_US.UTF-8
RUN touch /tmp/localdefed
VOLUME "/var/spool/docker"
CMD ["/usr/bin/supervisord","-c","/etc/supervisord.conf"]

