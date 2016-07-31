#
# From the demo sshd docker from docker.io
#

FROM centos:centos7

MAINTAINER "rob@duke.edu"

#
# Run a yum update, just for the sake of completeness
#

RUN yum -y update
#
#
## Layer in the VNC server
#
RUN yum -y install tigervnc-server
#
## Numpy is used to accelerate websockify
#
RUN yum -y install numpy
#
## Add in xfreerdp
#
RUN yum -y install xfreerdp
#
## And pyOpenSSL
RUN yum -y install pyOpenSSL
#
## And websockify (with some prereqs)
#
RUN yum -y install python-setuptools
RUN easy_install websockify
#
## And set up some things to allow it to run with SSL enabled
## Here we'll attach the self.pem file to make websockify SSL-enabled and from here we'll run websockify
RUN mkdir -p /var/www/html/novnc
#
## Install supervisor
#
RUN easy_install supervisor
#
## Set up home directory stub -- in this case, since we're a single container now, we need only have one homedir
## This will be the directory where the start file for the vnc X server is placed to initiate the RDP client
RUN mkdir -p /var/www/html/vnc-home
#
##setvncpass is a shell script that handles inserting the appropriate password data into the home directory before starting the VNC server
#
ADD setvncpass /tmp/setvncpass
#
##writexclients is a shell script that handles priming the .Xclients file in he home directory for VNC startup
#
ADD writexclients /tmp/writexclients
#
##runvncserver is a shell script wrapper that handles executing the vncserver
#
ADD runvncserver /tmp/runvncserver
#
## websocify wrapper
#
ADD runwebsockify /tmp/runwebsockify
#
## And start the supervisord
#
ADD supervisord.conf /etc/supervisord.conf
#
#
## Build up xfreerdp (since the version from RHEL7 is borked and old)
#
ADD FreeRDP /tmp/FreeRDP
RUN yum clean all
RUN yum install -y make
RUN yum install -y cmake
RUN yum install -y gcc
RUN yum install -y zlib-devel
RUN yum install -y zlib
RUN yum install -y xorg-x11-server-devel
RUN yum install -y xorg-x11-proto-devel
RUN yum install -y libXcursor-devel
RUN yum install -y openssl-devel
RUN yum install -y openssl
RUN yum install -y gcc-c++
RUN yum install -y avahi-glib
RUN yum install -y avahi-glib-devel
RUN yum install -y libXv-devel
RUN yum install -y libXi-devel
RUN yum install -y alsa-lib
RUN yum install -y alsa-lib-devel
WORKDIR /tmp/FreeRDP
RUN cmake -DCMAKE_BUILD_TYPE=Debug -DWITH_SSE2=ON
RUN make
RUN make install

WORKDIR /var/www/html/vnc-home
ADD self.pem /var/www/html/vnc-home/self.pem
RUN yum install -y mariadb
ADD cleanmeup /tmp/cleanmeup
RUN yum install -y net-tools
VOLUME "/var/spool/docker"
CMD ["/usr/bin/supervisord","-c","/etc/supervisord.conf"]

