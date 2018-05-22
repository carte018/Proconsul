# Dockerized Deployment

In this directory, you'll find a self-deploying, Dockerized version of Proconsul.  
This Dockerized deployment is designed to be as automated as possible, but there are
naturally some configuration details you'll need to be prepared for, and some pre-
requisites you'll need to satisfy before doing the deployment.  In testing, we've 
found that once the prerequisites are met and the required configuration file is 
populated, building an entire Proconsul instance from scratch with the provided build
script takes between 30 and 45 minutes, depending on the speed of your server and 
network.

Full instructions for the deployment process, along with tips for meeting the prerequisites
appear below.

## Step 1:  Preparing your server

You will need to prepare a Linux server to host your Proconsul instance (or possibly 
multiple Linux servers, if you want to deploy redundant Proconsul instances).  The 
prerequisites for deploying Proconsul as a Docker container are essentially the same
as those for deploying Proconsul directly on a host.  

You will need a Linux host (a VM is fine, as is a bare-metal server) running the 
Docker engine both for deploying the Dockerized Proconsul service and for Proconsul
to function normally (since Proconsul itself instantiates Docker containers to handle
user sessions).  At Duke, we use RHEL 7 servers to host Proconsul, but any Docker-capable
Linux server should work fine.  

If your site is already using Docker, chances are you have a "standard" Docker
installation, and more than likely, you can use it (possibly with one minor 
adjustment -- see below).  If you're not already using Docker, you may find this
document from [Docker docs][https://docs.docker.com/machine/install-machine/] useful.

Depending on your choice of Linux distributions and the exact Docker installation you 
use, you may need to make one mior adjustment to the way the `dockerd` daemon is 
started.  Proconsul depends on the Docker web service API to interact with the Docker
daemon, and because of restrictions in the Java bindings for the API, it requires that
the Docker daemon be configured to accept API requests via TCP on port 2375 
on the host's loopback interface.  Some Docker installations enable TCP for the API 
automatically; others don't.  Consult your local documentation to verify how to 
enable TCP in your specific situation.  For example, if you're running on a RHEL 7
system with the default docker package installed, you'll need to make sure that you
configure the `dockerd-current` process to run with the flags:

`--host=unix:///var/run/docker.sock --host=tcp://127.0.0.1:2375`

in order to support both the local Docker CLI (via the /var/run/docker.sock socket)
and TCP connections via port 2375 on the loopback interface (127.0.0.1).

Sizing is of course dependent on your anticipated workload.  At Duke, we've found that
a single, 2-core VMWare VM with 2GB of RAM and a 1Gbps network interface can handle 
up to around a dozen simultaneous user sessions (either RDP or VNC) without 
appreciable performance degredation.  A good rule of thumb for sizing your server may 
be to plan on providing one CPU core and one GB of RAM for the application server 
**plus** one CPU core and another GB of RAM for every 10 simultaneous user sessions
you expect to support.  For a server supporting up to 10 simultaneous users, a 2-core,
2GB system should be more than sufficient.  For a server supporting 50 simultaneous 
sessions, you might consider a 6-core, 6GB server, and so on.

Disk space requirements are likewise dependent on your usage patterns, but they are 
comparatively modest.  The Proconsul
docker image, plus a full complement of RDP and VNC client images, only require around
6 GB of storage.  Data containers built by the Dockerized installation process require
a minimum of around 1GB of storage, but audit trail logging and configuration 
may require a variable amount of additional storage, conditioned by the level of usage
and the length of time over which you wish to preserve audit logs.  At Duke, we 
preserve audit logs forever, but in over a year of active use, we've only accumulated
a few hundred KB of additional data.  

You will need root privilege on your Proconsul server in order to complete the 
installation.  This is the case whether you use the Dockerized installation outlined
here or you choose to build and install the code and its prerequisites directly on 
the host.  While there are other ways to get the code installed on your server, you'll
probably want to clone it from GitHub, in which case you'll need a Git client installed
on your server, as well.

You will need to arrange to permit incoming connections on TCP port 443 (for HTTPS
traffic to the Proconsul web interfaces) and a set of TCP ports starting at 5900 (for 
incoming VNC connections from web browsers).  The range of high-numbered ports you 
need to open depends on the number of simultaneous clients you wish to support -- each
client will require two adjacent port numbers starting at 5900 (eg., a single client
will use ports 5900 and 5901, while five concurrent clients will use ports 5900 - 5909).
At Duke, we typically configure individual Proconsul servers to handle up to 50 
concurrent sessions, so we allow traffic in on ports 5900 - 5999.  

There is no requirement to allow incoming traffic on TCP port 3306 (the MySQL port), 
but if you wish to interact with the Proconsul database remotely (eg., to inspect its
current list of active sessions and/or accounts for monitoring purposes) you may 
want to do so.

## Step 2:  Retrieving the Dockerized installation tree

If you're reading this README file, you're already in the right place.  Typically, 
you'll start by cloning the Proconsul project from GitHub:

`git clone https://github.com/carte018/proconsul`

To use the simplified Dockerized installation process, you'll then want to "cd" into
the `Dockerized` subdirectory, where you'll find a copy of this README file.

## Step 3:  Preparing your AD to support Proconsul

Proconsul can be used for a number of purposes, from managing privileged access within
and AD and reducing the attack surface for domain admin accounts within the AD to 
providing federated, zero-client-install, access-controlled remote login access to 
RDP- and VNC-capable hosts.  In each case, Proconsul does much of 
what it does by manipulating objects in an Active Directory.  As such, there is some 
preparation required in your AD in order to deploy and use Proconsul.

You will need to identify or prepare a few objects in your AD for Proconsul's use. As 
you identify or prepare them, you'll want to note some information about them in order
to complete the configuration step below.  Specific items you'll need to prepare are:

1. A UPN and password for Proconsul to use to bind to the AD.  This need not be a 
dedicated user for Proconsul's use, but it's strongly recommended -- using a bespoke
user for Proconsul will help avoid conflicting requirements between applications 
sharing the user account, and will allow you better audit options for actions Proconsul
performs in your AD.  **If you plan to use Proconsul to delegate domain admin 
access** the AD user that Proconsul binds with **must** have domain admin rights.  In 
that scenario, you may choose to make it the **only** persistent AD account with 
domain admin rights, but it needs that level of access in order to create and assign
domain admin rights to its dynamically-provisioned AD users.  Otherwise, this may be 
a "normal" AD user (to which specific rights will be assigned below).
2. An OU in the AD where Proconsul will provision and deprovision its dynamic users.  
This need not be a dedicated OU, but it is strongly recommended -- using a bespoke OU 
for Proconsul dynamic users will allow you to more easily track Proconsul activity,
and may allow you to limit the scope of rights the Proconsul server can exericise.
**If the bind user identified above is NOT a domain admin user** you will need to 
grant user management rights (at a minimum, create, delete, modify group membership, 
and reset password rights for user class objects) to that user in this OU.  
Proconsul will bind to the AD using the above user account and manipulate its dynamic 
users in this OU.
3. A group in the AD to which Proconsul will add its dynamic users when they are 
created.  This need not be a dedicated group, but it's strongly recommended -- using 
a bespoke group for this purpose will give you a simple and direct way to track 
Proconsul users from within the AD.  Depending on your use cases, you may find other 
uses for this group (eg., a common use case would involve adding this group to the 
RDP Users group of target Windows systems to allow all dynamically-provisioned 
Proconsul users RDP access to those hosts).  Regardless of whether this is a bespoke
or shared group, it **should not** be a highly-privileged group.  Proconsul can assign
arbitrary group memberships to its dynamic users based on its configuration and the 
properties of the "real" users it interacts with.  This group should be used only for 
general access and tracking purposes.
4. **Optionally** select an AD site within which to limit Proconsul's use.  If your 
AD environment supports multiple sites, you may find that limiting your Proconsul 
instance to accessing hosts in a single site provides better performance (at the 
expense of needing to operate a separate Proconsul instance for each site).  If your 
environment doesn't support multiple sites or if you want your Proconsul instance to 
span sites, you need not pick a site to use -- you may simply leave the associated 
configuration parameter empty in that case.
5. **Optionally** identify a non-standard group to use for domain admin privileging.  
In most cases, you will want to use the "standard" group `CN=Domain Admins,OU=Users,DC=your,DC=dom,DC=ain` to delegate domain admin rights to dynamically provisioned users.
In some circumstances, it may be desirable to define a secondary group for Proconsul
to use as its "domain admins" group, and add it to the "real" domain admins group. 
In either case, you will need to select a domain admin group for Proconsul to use.  
It will only actually be used in the event that you configure Proconsul to
manage delegated domain admin rights, but the tool will require a value for the 
configuration option, regardless.
6. **Optionally** create or identify a second user account for Proconsul's docker-gen
instance to use.  If you are not using a domain admin account as the bind user for 
Proconsul, you may simply re-use the bind user for this purpose.  If you are using 
a domain admin account for the former purpose, you may want to create a separate 
account with user management rights in the OU you created above for use by the 
docker-gen process.  Docker-gen is a separate process (courtesy of Jason Wilder) used
by the Proconsul distribution to handle clean-up of AD and MySQL artifacts when 
Proconsul's spawned Docker containers terminate.  A domain admin account will 
work just fine, but you may prefer to use a less privileged account with docker-gen
for purposes of compartmentalization.

## Step 4:  Preparing for configuration

### SSL certificate

Like any SSL-protected web application, Proconsul needs a public/private key pair and 
a signed SSL certificate in order to operate, and the Subject of the certificate
(or one of its SAN values) needs to match the name users will use when connecting to 
the service.  You will need to provide the names of two files -- one containing a PEM-
formatted version of the server's private key and one containing a PEM-formatted 
version of the SSL certificate you'll be using for your Proconsul server in order to 
build the Proconsul installation.  

### Federation information

Currently, Proconsul only supports operating as a relying party to a SAML federation 
or IDP.  Proconsul expects to authenticate users via SAML, and then rely on identity 
assertions provided by the users' SAML IDPs to determine what they're authorized to 
do. In future, other authentication mechanisms may be added as supported, but for now,
only SAML is supported.

The Dockerized deployment process automatically installs a Shibboleth SP and either 
copies in a host-level configuration (if one is already available) or creates a new 
entity configuration, complete with the necessary keys, etc.  You will need to decide
before setting up your configuration whether your Proconsul instance is going to be 
federated multilaterally (via a federation liek InCommon) or bilaterally (with a 
single, usually institutional identity provider).  Both modes are suppported.  In 
either case, you will need to select a unique SAML entity ID for your Proconsul
service.  In the bilateral federation case, you will need to collect the SAML entityId
of your chosen SAML identity provider, while in the multilateral federation case, you 
will need to collect the URL of a suitable discovery service for use within your 
federation.  In both cases, you will need to provide a URL from which to retrieve 
metadata (in the bilateral case, for your chosen IDP, and in the multilateral case, 
for your entire SAML federation) and a file containing the certificate with which to 
verify the signature on your IDP's or federation's metadata.

Contact your local SAML IDP operator for help getting this information if you don't 
have it already.


## Step 5:  Construct the master configuration

In the same directory as this README file, you should find a file named 
`master.config.sample`.  Make a copy of this file, and following the instructions
noted in the comments in the file, populate appropriate values for the parameters
therein.  Each parameter is set with a line that looks like:

`parameter="value"`

You may run the build process without any prior configuration, in which case a set 
of default values will be used where possible, and you will be prompted to enter 
values for parameters that must be tuned to your local environment.  You may also 
provide a configuration file with only part of the configuration required, in which 
case the remainder of the configuration will be defaulted or prompted for as needed.
It's recommended, though, that you fully populate the configuration, to allow the 
build process to run (largely) unattended.

For reference, the settings (in brief) are:

**fqdn** - the hostname users will use in URLs to contact your Proconsul instance  
**certfile** - full pathname to a file containing your SSL certificate  
**keyfile** -- full pathname to a file containing your SSL private key  
**adldapurl** -- LDAP URL for connecting to your AD (usually "ldaps://domain.name:636")  
**adbinddn** -- UPN of the bind user Proconsul should use in the AD  
**adbindcred** -- password for the user in adbinddn  
**addomain** -- DNS name of your AD domain (eg. "my.dom.ain")  
**adsearchbase** -- DN of the root of you AD domain (eg., "DC=my,DC=dom,DC=ain")  
**usedelegatedadmin** -- Unless you're at Duke, enter "n" here.  
**addeptbase** -- Unless you're at Duke, leave this empty  
**adorgbase** -- Unless you're at Duke, leave this empty  
**adtargetbase** -- OU where Proconsul will create its dynamic users (full DN)  
**adldapdcs** -- comma-separated list of LDAP URLs for individual DCs in your domain  
**addagroupdn** -- DN of the AD group to use as the domain admins group   
**adproconsuldefgrp** -- DN of the AD group Proconsul should add its dynamic users to   
**adsiteprefix** -- AD site (or unique prefix thereof) to restrict Proconsul to (if any)  
**rdpdockerimage** -- name/tag to use for the normal-sized RDP client container  
**rdplargedockerimage** -- name/tag to use for the large-sized RDP client container  
**vncdockerimage**  -- name/tag to use for the normal-sized VNC client container  
**vnclargedockerimage** -- name/tag to use for the large-sized VNC client container  
**dockerhost** -- IP address of the host where Docker containers should be run (almost always 127.0.0.1)  
**dockercpuset** -- cpuset value for your dockerhost (eg., "0-1" for a 2-core server)  
**novnchostname** -- host where novnc connections should be sent (usually == $fqdn)  
**mysqlhost** -- IP of host where Proconsul MySQL server runs (usually 127.0.0.1)  
**proconsuluser** -- MySQL userid to create for use with Proconsul schema  
**proconsuldbpw** -- password to use for proconsuluser in the MySQL DB  
**logouturl** -- URL to anchor behind the "sign out" link on Procosnul pages  
**pcadminlist** -- list of REMOTE_USER values for users who should have rights to the Proconsul administrative UI -- PROBABLY NEEDS TO INCLUDE YOURS  
**authnmode** -- authentication mode (for now, this **must** be "saml")  
**federationmode** -- "bilateral" to federate with one IDP, "multilateral" for a full SAML federation  
**spentityid** -- unique entityID to use for the Proconsul SAML relying party  
**idpentityid** -- entityID of your IDP (if bilateral above); empty otherwise  
**discoveryurl** -- URL of SAML federation discovery service (if multilateral federation -- empty otherwise)  
**federationmdurl** -- URL to retrieve IDP or federation metadata  
**mdsigningkey** -- file containing certificate to validate metadata signature  
**dockergenuser** -- UPN of AD user for docker-gen to use for cleanup  
**dockergenpw** -- password for dockergenuser  

## Step 6:  Build your Docker containers

For a first-time build, on a newly deployed Docker engine server with no prior 
configuration and no pre-build Docker images, become "root" on your Proconsul server 
and run the "build" script in this directory.  You can check the syntax by running:

`build --help`

In general, for a first-time build, you'll likely want to run either:

`./build -c config-file-name -q`

to provide as little information during the build as possible, ask as few questions 
during the build as possible,  and use configuration settings from "config-file-name"
or:

`./build`

to run with as much information on-screen as possible and prompting for all 
configuration options as they are needed.

Note that if you run with the "-q" option, you won't be prompted for any unnecessary 
information during the build, but you may still have to enter information or respond
to prompts during the build process, either because of missing information in your 
configuration file or because the build process creates a new SAML relying party key
that you'll need to record and share with your federation and/or IDP operators to 
complete your federation configuration.

The build process will produce a collection of Docker images and Docker volumes on 
your Linux machine:

`carte018/proconsul` is the Docker image/container for running Proconsul.  The build 
process compiles and builds WAR files for the two Java Web applications (Proconsul and
the ProconsulAdmin UI), and sets up a Shibboleth SP and associated Apache daemon to
run in front of a Tomcat instance where the two Java Web applications are hosted.  
A docker-gen instance is also started with configuration arranged to perform clean-
up after sessions are terminated.

`carte018/rdpstack` (or whatever you choose to use for $rdpdockerimage) is the Docker
image/container Proconsul instantiates to make "small-screen" connections to RDP 
servers.  The container includes an XFreeRDP client, an XVNC server, a websocket 
forwarding daemon, and some associated software.

`carte018/rdpstacklarge` (or $rdplargedockerimage) is the equivalent of the rdpstack
image above with configuration settings for large screens (1900x1200 resolution).

`carte018/vncstack` (or $vncdockerimage) is the equivalent of the rdpstack image above
for use with VNC servers rather than RDP servers.  It includes a VNC viewer client 
rather than XFreeRDP and an SSH client to facilitate start-up of remote VNC servers.

`carte018/vncstacklarge` (or $vnclargedockerimage) is the equivalent of the vncstack 
image above with configuration settings for large screens (1900x1200 resolution).

Depending on the command line arguments used and what configuration is or is not 
provided, you may be prompted to enter information and/or record information for 
later use during the build process.  The build process typically takes approximately 
30 minutes for a "complete" build, although your timing may vary depending on 
the performance of your host and network.

## Step 7: Complete your federation enrollment

During the build process, a Shibboleth SP will have been configured in the Proconsul
Docker image.  If the build process created a new entityID and key pair for the 
SAML endpoint, you should have been given an opportunity to save the associated 
certificate information (in PEM format) during the final stage of the build.  If your 
server already had an /etc/shibboleth directory with an SP configuration including 
SP private key and self-signed certificate, that will have been copied into the 
Proconsul Docker container to configure the Shibboleth SP it will run.  In either 
case, you will need to share your chosen entity ID and the associated certificate with
your IDP operator (if you're federating bilaterally) or your federation operation (if
you're federating multilaterally) in order to complete the authentication set-up for 
your Proconsul instance.  Other information (privacy policies, icon URLs, etc.) may 
be required, depending on your federation operator and/or IDP operator.

## Step 8: Start the Proconsul instance

At this point, you're ready to start your Proconsul instance.  You may do this using the
`run3.sh` script provided in this directory.

## Step 9: Configure Proconsul itself

If everything's worked correctly, you should be able to point a web browser at the URL:

https://your.proconsul.server/admin/

to begin configuring your Proconsul instance.  You should be required to authenticate 
via your SAML federation, and provided that you included your own identity in the list
of Proconsul admins in `$pcadminlist`, you should see the Proconsul admin UI.

See the documentation (below) for the Proconsul admin interface for more information regarding 
managing your Proconsul instance via the admin UI.

## Updates and rebuilds

From time to time, you may need to rebuild your Proconsul Docker image and/or the associated
data containers.  

By default, the `build` script in this directory will perform a complete rebuild of your 
Proconsul environment -- including wiping out your Shibboleth SP configuration and your 
mySQL databases.  Usually, that's something you'll want to do only once, the first time you 
build Proconsul on your machine.

To rebuild Proconsul without removing data already loaded into the Proconsul database and
without removing the Shibboleth SP configuration, use the `-r` flag to the `build` command.  
For example:

`./build -c config-file-name -r`

will rebuild the software components of Proconsul without removing or replacing your 
mySQL database or Shibboleth SP configuration.  The similar:

`./build -c config-file-name -r -q`

will do the same but suppress unnecessary prompts.  

## A note about data container backups

Using the fully Dockerized installation for Proconsul has advantages in terms of ease of 
installation and maintenance, but it may make backing up your local configuration data
(particularly the data in the Proconsul database) more difficult.

You may find it helpful to periodically dump the contents of your Proconsul database to storage
outside your Dockerized environment for purposes of backup.  One easy way to do this is to run 
the following command on the host you're running the Dockerized Proconsul instance on:

``docker exec -i -t `docker ps | grep carte018/proconsul | grep 'Up' | awk '{print $NF}'` mysqldump -u mysql-user -p -A > backup-file-name.sql``

You'll need to replace `mysql-user` with the username you specified in your configuration as
the MySQL user for Proconsul to create and use, and you'll be prompted for its password when
the mysqldump command executes.  `backup-file-name.sql` will be created containing the SQL
commands necessary to reproduce the current state of your Proconsul database.


# Proconsul Admin Interface

Your Proconsul instance consists of two distinct web applications -- Proconsul itself (found
at the URI `/proconsul/`) and the Proconsul admin interface (found at the URI `/admin/`).  The
Proconsul admin interface is accessible only to users you have explicitly listed in the 
Proconsul admin configuration (usually by setting the value of `$pcadminlist` during the 
initial installation.  The Proconsul application itself is accessible only to those users 
who are authorized to it through the Proconsul admin interface.

Your first step, then, after building and starting Proconsul on a new system, will be to visit
the admin interface and set up the user(s) and system(s) Proconsul will interact with.

Point your browser at:

`https://my-proconsul-server.my.dom.ain/admin/`

and complete your SAML login.  You should be presented with the Proconsul Admin interface, 
which offers five collections of settings with which you may manage access to your Proconsul 
instance.  Here, we'll cover each of the collections in the order they appear on the page.  You
may configure each collection of settings separately, and in any order you wish.

## Proconsul Access Rules

The Proconsul Access Rules panel in the Proconsul admin interface allows you to control which 
authenticated users are allowed to interact with the Proconsul application and which (if any) of
them are authorized to make Domain Admin connections to hosts (rather than "normal user" 
connections). 

Click the "Proconsul Access Rules" link on the admin site's main page to review and edit the 
access rules for your Proconsul instance.

You will see two sub-panels -- one for configuring Proconsul Login Rules (which control what 
users are authorized to even access the Proconsul application) and one for configuring 
Domain Admin Access Rules (which control Domain Admin rights delegation).  In order for a user
to log in to Proconsul at all, he or she must meet the requirements for at least one of the 
Proconsul Login Rules specified here.  In order to additionally gain Domain Admin rights via
Proconsul, the user must also meet the requirements for at least one of the Domain Admin 
Access Rules specified here.  Initially, there will be no rules in the database, and no users 
will have rights to use Proconsul.

The Add Login Access Rule button will open a dialog box in which you may specify a rule for 
users to match in order to log in to Proconsul.  Access rules consist of property-value pairs
with three properties available via the pull-down menu in the dialog box:

* Username  -  used to explicitly match the value of REMOTE_USER passed to Proconsul with a user logs in.  Use this to explicitly grant rights to a single user.
* Group URN -  If your SAML IDP (or the IDPs in your federation) release isMemberOf values to your Proconsul instance, you can use this property to match a value of isMemberOf asserted for the user in order to authorize the user to log in.  Use this to grant rights to all members of a group by specifying the isMemberOf value Proconsul will see for its members at login.
* Entitlement - If your SAML IDP (or the IDPs in your federation) release eduPersonEntitlement values to your Proconsul instance, you can use this property to match a value of eduPersonEntitlement asserted for the logged-in user in order to grant login access.  Use this to grant rights to users with a specific entitlement asserted at login.

You can mix and match access rules, and rules may overlap (that is, one user may match multiple
rules.  Note that a Proconsul Login Rule only grants user(s) the ability to log in -- once
not to access any specific system or acquire any specific group membership on login.

If you plan to use Proconsul to delegate Domain Admin rights to users during sessions, you will 
need to additionally add Domain Admin Access Rules to grant that access to those users.  The 
Add Domain Admin Access Rule button will open a dialog box similar to the Add Login Access Rule 
dialog and allow you to specify rules to enable user(s) to use the Domain Admin portion of the 
Proconsul application.  Domain Admin Access Rules are just like Login Access Rules, and may 
match by Username, Group URN, or Entitlement.

If you wish to remove an existing Login or Domain Admin Access Rule, simply click the "Remove"
button next to it in the Access Rules panel.

## Proconsul Target Systems

Once a user is logged in to Proconsul, the user's access is largely controlled by the Host 
Mapping rules configured in the Proconsul Target Systems panel.  Click the Proconsul Target 
Systems link on the main admin page to configurat target systems for users of Proconsul.

Similar to the Login Access panel, you'll see two sub-panels on the Host Mappings page -- one
showing normal host mapping rules in force in your Proconsul instance, and one showing 
Domain Admin Hosts configured for your Proconsul instance. 

Proconsul restricts the hosts its users can establish connections two in two different ways:

1. Normal (non-domain admin) connections are controlled by Host Mapping rules.  In order to access a given target system, a user must match one of the host mapping rules associated with that system (or with the OU in the AD where that system's computer object resides).
2. Domain Admin connections are limited to designated Domain Admin Hosts.  Any user with Domain Admin access in Proconsul can establish a connection to any host in the list of Domain Admin Hosts, regardless of any Host Mapping rules that may be in effect.

Click the Add Host Mapping Rule button to add a host mapping rule for normal connections.  Host
mapping rules consist of two property-value pairs -- one controlling what user(s) the rule 
applies to and one specifying what host(s) the rule applies to.

You may configure target host access in a similar fashion to how you control login access.  You
may specify a rule that matches a user's Username, Group URN, or Entitlement.  You may specify
a single target host by selecting "Hostname" and setting the associated value to the fully-
qualified domain name of the target host.  You may specify a collection of target hosts by 
selecting the AD OU option and setting the associated value to the distinguished name (dn) of
an OU in the AD containing one or more Computer objects (in which case, matched users will 
be allowed to connect to _all_ of the systems with objects in that OU).

Domain Admin Hosts are configured explicitly.  Clicking the Add DA Host button will allow you 
enter the fully qualified domain name of a host you wish to allow Domain Admin users to connect
to with full Domain Admin rights.

To remove either a Host Mapping Rule or a DA Host, simply click the Remove button next to it in 
the admin display.

## Proconsul POSIX User Management

If you plan to use Proconsul to delegate remote desktop access to POSIX systems (Linux, etc.),
you will need to tell Proconsul what POSIX attributes should be used for users when accessing
those systems.  If you plan only to use Proconsul for Windows target systems, you can ignore 
this admin panel.

Click the Proconsul POSIX User configuration link to open the POSIX User Management panel.

The Add POSIX User Mapping button will allow you to add POSIX attributes for a user.  The 
resulting dialog box will allow you to enter:

* Username  -  the REMOTE_USER value Proconsul will see for the user in question
* Uid Number - the POSIX uidNumber value to assign to dynamic users created on behalf of this user
* Gid Number - the POSIX gidNumber value to assign to dynamic users created on behalf of this user
* Home Directory - the POSIX homeDirectory value to assign to dynamic users created on behalf of this user
* Login Shell - the POSIX loginShell value to assign to dynamic users created on behalf of this user

Note that you may specify overlapping entries for different users.  That is, you may arrange to 
have both `userA@domain` and `userB@domain` get the POSIX uidNumber `2027`, in which case, 
when _either_ userA or userB logs in to Proconsul and accesses a POSIX-dependent system, he
or she will appear to be a single user (with uidNumber `2027`) on that system.  This can be 
useful for certain shared administrative use cases.

To remove a POSIX user mapping, click the "Remove" button next to it in the admin display.

## Proconsul/AD Group Configuration

In addition to granting full Domain Admin rights to specified users for specific remote 
login sessions, Proconsul can be used to grant arbitrary group memberships to users for the 
duration of single login sessions on target systems.  This is essentially just a generalization
of Proconsul's Domain Admin capability.

Click the Proconsul/AD Group Configuration link on the main admin panel to manage AD group 
delegations in Proconsul.

On the Proconsul AD Group Management page, you can specify mappings from users (or collections 
of users) to AD groups that will be applied when dynamic users are created on those users' 
behalf.

Click the Add AD Group Mapping button to add a group mapping rule.  In the resulting dialog box,
you can specify a user or users as in the other panels (by Username, Group URN, or Entitlement)
and specify the distinguished name (dn) of a group object in the AD.  When a user matching the
user property in the rule connects to a target system as a non-Domain Admin, Proconsul will
add the dynamic user it creates to the specified AD group before logging the dynamic user in to
the target system.

If a single user matches multiple AD Group Mapping rules, Proconsul will add the dynamic user
created for her to _all_ of the matched groups.  A single user may match multiple rules in order
to be added to multiple groups, or may match rules targeting the same group in multiple ways
(in which case Proconsul will still only add the dynamic user to the group once).

These settings are ignored when users invoke Domain Admin privileges, since Domain Admin rights 
are generally a supeset of the rights afforded by other group memberships.

To remove an AD Group Mapping, click the Remove button next to it in the admin display.

## Target Provisioning

Some target systems may require special consideration by Proconsul.  Some hosts may require 
their users to be members of specific access control groups, and some may be configured to 
only be accessible via an RDP gateway.  Click the Target Provisioning link on the main admin
panel to configure host-specific parameters for Proconsul.

You will see two sub-panels on the Target Provisioning page - one for Proconsul Host Groups and
one for configuring Proconsul RDP gateways.

Host Access Group configuration allows you to specify an AD group to which _any_ dynamic user 
making a connection to a particular host (or collection of hosts) will be added.  Typically, 
this feature is used to ensure that Proconsul dynamic users are members of target systems' 
RDP Users group in order to authorize their RDP access, but it may be used in any situation in 
which logging in to a target host should dictate what groups a dynamic user is made a member 
of.

Click the Add Host Access Group button to add a host access group rule.  The resulting dialog
will allow you to specify a host (by fully qualified domain name) or collection of hosts (by
AD OU DN) and the DN of a group.  Once enacted, the rule will cause Proconsul to add any 
dynamic user it creates for a session on (one of) the target machine(s) to the specified group.

You may specify multiple overlapping Host Access Group rules -- if multiple rules apply to a 
given host, dynamic users accesing that host will be added to all the relevant groups.

RDP Gateway configuration allows you to specify an RDP gateway host that must be used for RDP
connections to target systems.  Click the Add RDP Gateway Config button to add an RDP Gateway
rule.

The resulting dialog will allow you to specify the fully qualified domain name of a target 
system, the fully qualified domain name of the RDP gateway all connections to that system
should be routed through, and the DN of an AD group to which users must be added in order to 
access the RDP gateway.  When an authorized user requests a session on the target system, 
Proconsul will add its dynamically-created user to the RDP gateway group (in addition to any
other groups mandated by other rules) and route the RDP connection via the specified 
RDP gateway.

To remove either a Host Access Group mapping or an RDP gateway configuration, click the Remove
button next to it in the admin display.
