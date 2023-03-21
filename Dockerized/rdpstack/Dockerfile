#
# From the demo sshd docker from docker.io
#

#FROM centos:centos7
FROM fedora:36

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
## Add in xfreerdp -- this merely provisions the OS to run the software, 
## since we replace the code later in the build
#
RUN yum -y install xfreerdp
#
## And pyOpenSSL
RUN yum -y install pyOpenSSL
#
## And websockify (with some prereqs)
#
RUN yum -y install python-setuptools
#RUN yum -y install epel-release
RUN yum -y install python-websockify
#RUN easy_install websockify
#
## And set up some things to allow it to run with SSL enabled
## Here we'll attach the self.pem file to make websockify SSL-enabled and from here we'll run websockify
RUN mkdir -p /var/www/html/novnc
#
## Install supervisor
#
#RUN easy_install supervisor
RUN yum -y install supervisor
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
#ADD FreeRDP /tmp/FreeRDP
RUN yum install -y git patch
WORKDIR /tmp
RUN git clone https://github.com/FreeRDP/FreeRDP
WORKDIR /tmp/FreeRDP
RUN git fetch --all --tags
#RUN git checkout tags/2.1.1 -b 2.1.1-master
RUN git checkout master
#ADD nla-patch /tmp/nla-patch
#RUN patch -p1 < /tmp/nla-patch

RUN yum install -y procps-ng

RUN yum clean all
#RUN yum install -y epel-release
#RUN yum localinstall -y --nogpgcheck https://download1.rpmfusion.org/free/el/rpmfusion-free-release-7.noarch.rpm
RUN yum install -y gcc cmake ninja-build openssl-devel libX11-devel libXext-devel libXinerama-devel libXcursor-devel libXi-devel \ 
libXdamage-devel libXv-devel libxkbfile-devel alsa-lib-devel cups-devel ffmpeg-free-devel glib2-devel xorg-x11-server-devel \
openssl-devel openssl gcc-c++ avahi-glib avahi-glib-devel libusb-devel
WORKDIR /tmp/FreeRDP
RUN cmake -GNinja -DCMAKE_BUILD_TYPE=Release -DCHANNEL_URBDRC=OFF
RUN cmake --build .
RUN cmake --build . --target install



WORKDIR /var/www/html/vnc-home
ADD self.pem /var/www/html/vnc-home/self.pem
RUN yum install -y mariadb
ADD cleanmeup /tmp/cleanmeup
RUN yum install -y net-tools
VOLUME "/var/spool/docker"
CMD ["/usr/bin/supervisord","-c","/etc/supervisord.conf"]

