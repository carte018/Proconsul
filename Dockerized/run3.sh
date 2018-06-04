mkdir -p /srv/system_logs/supervisor
mkdir -p /srv/system_logs/httpd
mkdir -p /srv/system_logs/tomcat7
mkdir -p /srv/tomcat7/webapps
mkdir -p /srv/www
mkdir -p /etc/proconsul

# --net=host

docker run --restart=always -d -i -p 443:443 -p 3306:3306 -e TZ=America/New_York -v proconsul-log-data:/var/log -v proconsul-webapps-data:/var/lib/tomcat7/webapps -v proconsul-shibboleth-data:/etc/shibboleth -v proconsul-etc-data:/etc/proconsul -v proconsul-mysql-data:/var/lib/mysql -v proconsul-spool-data:/var/spool/docker -v proconsul-docker-gen:/opt/docker-gen -e "HOSTIP=`hostname -i`" -t carte018/proconsul

