# Proconsul Architecture

## Overview

At a very high level, Proconsul is a server-side Java web application that manages dynamic user identities and dynamically-spawned remote desktop client environments, and securely brokers connections to those desktop environments through a user's web browser.  It's been used successfully to solve a number of different classes of use case, including:

* Providing zero-install, clientless remote login access to Windows and Linux hosts
* Providing tightly-controlled desktop access to secured servers residing behind a non-porous firewall
* Providing a bridge between federated identity (eg., SAML) and remote desktop access to Active Directory-dependent systems (both Windows and Linux)
* Providing a mechanism for privileged account management in an Active Directory environment that can dramatically reduce domain credential exposure

Applications have ranged from administrative (Duke uses Proconsul as the primary mechanism for system adminstrators to operate with Active Directory Domain Admin authority) to research (eg., federated access to tightly-controlled RDP connections).

This document (and the associated PDF diagram) will attempt to describe in some technical detail the overall architecture of a deployed Proconsul instance.

A working Proconsul instance comprises a few primary components:

* An HTTP server capable of securely authenticating end users and hosting Java applications.  
* A server-side Java web application (the user-facing "Proconsul" application itself)
* A server-side Java administrative web application (the admin-facing "proconsuladmin" application)
* A (usually SQL) database (for storing configuration, access policy, and auditing information)
* A Docker service (for managing containerized RDP and VNC clients used to establish remote desktop connections on behalf of Proconsul users)
* A docker-gen service (for monitoring the status of spawned remote desktop sessions, and performing administrative cleanup as sessions are terminated)
* An Active Directory domain which is relied upon by one or more target hosts (to which Proconsul can then broker remote desktop sessions).  One Proconsul deployment can only be associated with one Active Directory domain.  To support multiple AD domains, you must deploy multiple Proconsul instances.

We'll duscuss each component in some detail, below, before describing a typical Proconsul session lifecycle.

## Components, in detail

### HTTP Server and Java container

Proconsul is a web-based application, and as such, it needs to be hosted by some sort of HTTP server.  The two web user interface applications it comprises are Java applications that in turn need to be run in a Java servlet container.  The Dockerized "simple" deployment packaging currently uses an Apache HTTP server and a Tomcat instance for this purpose, but Proconsul itself is agnostic about HTTP server and Java servlet container types.  There are a few stipulations that must be met by the HTTP and servlet servers used with Proconsul:

1. The HTTP/Java environment must be able to provide authentication of end users.  In the current release, only SAML is fully supported as an authentication scheme, and a Shibboleth SP is provided as part of the "simple" Dockerized deployment to facilitate this.  (Note:  Proconsul does not provide a SAML identity provider -- a source of federated authetnication, either a local IDP or membership in a SAML federation, is required to be established separately).  With only limited adjustment, however, Proconsul can be made to work with any container- or HTTP server-based authentication mechanism.
2. The HTTP/Java environment should be able to provide SSL support.  This is not a strict requirement, but since Proconsul is often used in high-security environments, SSL is usually highly desirable.

In a working Proconsul deployment, there are two Java applciations (pacakged as WAR files) served by any Proconsul server -- the "proconsul" application (which end users interact with when setting up and accessing proxied remote desktop connections) and the "proconsuladmin" appliation (which system administrators use to configure Proconsul and manage access and authorization policies).

### Proconsul Java application

The Proconsul Java applicatication acts as the orchestration mechanism for the Proconsul system.  It presumes that its user has been authenticated by the time it receives the user, and relies on the servlet container to provide it with information about the authenticated user.  At a minimum, Proconsul needs to receive a unique identifier for the user (presented as the RemoteUser property in the servlet request the application receives), but if the environment supports it, Proconsul can take advantage of additional identity information provided via the servlet container, including group membership information and entitlement information.  As currently delivered, the Dockerized "simple" deployment expects to receive a scoped user identifier (usually eduPersonPrincipalName, but possibly antoher unique, persistent identifier -- eduPersonTargetedId, for example) in the RemoteUser property and expects to find SAML attribute assertions in HTTP headers for additional identity information.  At the moment, only group membership presented as (possibly multiple) values of the "isMemberOf" attribute and entitlements presented as (possibly multiple) values of the "eduPersonEntitlement" attribute are supported, in keeping with the expectation that most deployments will be using SAML, and that most SAML federations support eduPerson attributes as SAML assertions.

The Java application requires:

* LDAP(s) access to the Active Directory DCs for the domain in which it operates.  A Proconsul instance *may* be restricted to operating in a single site within an AD domain, in which case the Java application will only require LDAP access to the DCs serving that site.  In the site-restricted case, the Proconsul instance can only broker access to servers and workstations in that site.  Any firewalls, etc. between the Proconsul web server and the AD DCs must permit LDAP traffic between the two.
* An authorized account (often a "service account") in the Actie Directory.  The level of authorization required will vary depending on exactly how Proconsul is being used.  As a rule of thumb, if Proconsul will be used to broker access to Domain Admin sessions, Proconsul's AD service account will need Domain Admin rights (or the equivalent), while if it will be used to broker non-Domain Admin sessions exclusively, its service account may only need limited account administration rights (typically within a single OU in the directory).
* HTTP access to the Docker engine running on Proconsul's Docker server.  Proconsul spawns a Docker container to handle each user session it establishes.  It does this using the WS API provided by Docker.  In most installations, the Docker server is colocated with the Proconsul web application, although that's not strictly required -- in either case, though, Proconsul interacts with its Docker server via HTTP(s).
* TCP access to the database server housing Proconsul's authorization and configuration information.  Often, this will be a MySQL server that may be coresident with the Proconsul web server, but it can also be a different flavor of SQL server, or on a different machine (or set of machines, in the case of clustered database configurations).  In the Dockerized "simple" deployment, we package a MySQL server coresident with the Prconsul web application for this purpose.

### Proconsuladmin Java application

The proconsuladmin Java application provides a browser-based interface for configuring authorization and access controls for an associated Proconsul application instance.  Essentially, it's a front-end to managing data in the Proconsul database.  Its requirements are similar to those for the Proconsul application itself:

* An authenticated Java servlet environment
* TCP access to the Proconsul database service

It does not interact directly with the AD, so it needs no access to the AD DCs for the associated domain, nor does it interact with the Docker server to start or stop Docker containers, so it needs no access to that interface.  In most cases, a proconsuladmin instance is coresident with a proconsul web application instance, although the two can be run in separate locations if that's desired.

### Proconsul Database

The Proconsul database provides a number of critical services for a Proconsul installation.  The database:

* Holds basic access control policies governing which authenticated users can access the Proconsul web application at all.  Requests from unauthorized or unauthenticated users are rejected.
* Holds session information (including keys used to defeat CSRF attacks against the web applications) for the web applications.
* Holds target configuration information dictating what target hosts can be accessed by what Proconsul users
* Holds user POSIX information (where applicable) for Proconsul users
* Holds remote login session information (including the mapping between logged-in users, login sessions, and active remote login sessions with target systems) for active and disconnected-but-reconnectable remote desktop connections
* Holds port mapping information and VNC key information for individual remote login sessions. 
* Holds audit logs for remote desktop sessions, including mappings from logged-in users to dynamic session users, session start/disconnect/stop records, and group membership information.
* Holds authorization information controlling the group memberships assigned to dynamically-provisioned AD users on behalf of logged-in Proconsul users.
* Holds additional group membership requirements (where applicable) pertaining to specific target hosts served by a Proconsul instance.
* Holds target host configuration information (including RDP gateway information).

Proconsul depends on an Active Directory domain to provide it a means for managing access to its target system.  It depends on its own database to manage access policies.  As such, access to the database is just as critical for the function of Proconsul as access to the AD DCs, and securing access to the database is just as important as securing access to the AD itself.  In the Dockerized "simple" deployment, we use a MySQL database coresident with the Proconsul server and accessible only via local connections on the host. 

### Docker service

Proconsul establishes remote desktop connections with target systems using custom Docker containers, one of which it instantiates for each end-user connection it brokers.  In most deployments, the Docker service it uses to instantiate these containers will be coresident with the Proconsul Java application (and that is the configuration provided in the Dockerized "simple" deployment), but in very complex deployments, the Proconsul applicatoin can act as a gating proxy to Docker containers instantiated on another or other server(s).

Because Proconsul relies on a set of Java bindings to the Docker API that only support TCP-based connections to the Docker engine, Proconsul requires that the Docker engine it interacts with listen for TCP connections (typically on port 2375), even if the Docker server is coresident with the Proconsul application.

The Docker containers spawned by Proconsul use a Docker image that runs:

* A supervisord daemon, which is used to instantiate the other processes in the appropriate fashion
* An instance of Xvnc, which provides an X server with a virutal in-core frame buffer and a VNC server for remote access
* An instance of XfreeRDP (for RDP connections) or an instance of vncViewer (for VNC connections) to connect to target systems
* An instance of websockify, to handle incoming VNC traffic from noVNC clients running in web browsers and translate the websock traffic into usable VNC traffic for talking to the Xvnc VNC servers as needed
* A set of scripts that pre-configure Xvnc and capture and persist in the container's overlay filesystem information about the exit code generated when the XfreeRDP client exits.

There are a total of four "flavors" of Docker image, two for each remote desktop protocol (RDP and VNC), that differ only in the virtual screen size they support.  The Dockerized "simple" deployment process automatically builds all four Docker images and records their image names automatically in the Proconsul configuration for later use.

When Proconsul instantiates a Docker container for an individual connection to a target system, it passes into the container (vie the container's environment) a set of parameters that uniquely determine how the container will behave and how it can be accessed.  These include:

* the sAMAccountName of the dynamic AD user created for use by the specific session
* the credential (password) generated for that dynamic user
* the domain name of the AD domain
* the VNC password generated for that specific session
* the hostname of the target host (fqdn)
* the X display number assigned to the container for the specifc session
* the port numbers (internal and external) on which websockify and the container's VNC server are to listen
* the RDP gateway fqdn (if one is required for the connection)

That information is used by initialization scripts inside the Docker container to configure the processes it runs for the specific session parameters required.

### Docker-gen service

Proconsul creates and authorizes AD users dynamically as they are needed to support connections to target systems, but the expectation is that thes edynamic identities in the AD will be ephemeral, lasting only for the duration of a single desktop session.  Each time I use Proconsul to access target host X, I log in as a different, distinct AD user that's created exclusively for that session, and that's destroyed as soon as that session is terminated.  In this way, it is possible to have AD credentials suitable for accessing restricted systems or carrying restricted privileges *only* when they are in active use by authorized users.  At steady state, there may be no AD users authorized to access a particular host, for example -- when a Proconsul user authorized to access the host initiates a session, a dynamic identity is created for the session, granted the appropriate group memberships in the AD domain, and then used to initiate a desktop session with the target host.  When the session ends, the AD identity is removed.

While the Proconsul web application handles the creation and group management of these dynamic AD users, it does not directly handle their removal from the AD.  The Proconsul web application is not in-line with the remote desktop session once it is established -- the user's browser connects (via VNC) directly to an active Docker container running the appropriate RDP (or VNC) client.  As such, the Proconsul application isn't able to detect when a user's desktop session ends.

For that, Proconsul relies on an instance of docker-gen coresident with its Docker server.  The docker-gen daemon listens to the message stream generated by the Docker server and monitors it for "shutdown" events pertaining to Docker containers.  When a Docker container is shut down, the docker-gen daemon collects its unique identifier and uses it to inspect the stopped container's filesystem to determine the disposition of the XFreeRDP (or vncViewer) client when it terminated and the environment variables that were passed into the container at its instantiation.  If the client terminated because of a "logout" event, the docker-gen daemon removes the dynamic AD user created for the session before reaping the Docker container and updating the Proconsul database to reflect the session as logged out.  If the client terminated because of a "disconnect" event, the docker-gen daemon randomizes the credential of the dynamic AD user (but leaves it in the AD, since it is still logged in on the target system), and marks the Proconsul session as "disconnected".

## Typical session lifecycle

It may be instructive to examine in some detail what the sequence of events that occur during a typical user session in Proconsul looks like.  For the sake of discussion, we will assume that the deployment is roughly similar to the Dockerized "simple" deployment, with a Shibboleth SP providing SAML authentication services to an Apache server acting as an authenticating proxy in front of a Tomcat servlet container running the Proconsul web application.  We'll stipulate that an AD domain has been properly configured and that all the appropriate base configuration has already been done for PRoconsul, and we'll look at a series of operations performed by a Proconsul user ("user") accessing a target Linux system ("target") via RDP.  The sequence of events is substantially similar for a user accessing a Windows target system via RDP, or for a user accessing a Linux target system via VNC with minor variation in what Docker containers are invoked, etc.

Starting with user invoking a fresh, previously unauthenticated web browser:

1. User visits the Proconsul application URL (typically something like "https://proconsul-server/proconsul/") in an HTML5 capable browser.
2. Because the Proconsul application URL is proctected by a SAML endpoint (in this case, a Shibboleth SP), the user's browser is redirected to a SAML IDP (or to a federation discovery service) with an authentication request from the Proconsul server.
3. User authenticates at the appropriate IDP and user's browser is redirected to the Proconsul server with a SAML response containing at least the user's unique identifier (in this case, an eduPersonPrincipalName) and possibly additional information (isMemberOf and eduPersonEntitlement values).
4. The SAML endpoint (the Shibboleth SP) validates and consumes the SAML response, and places the assertion information in the Apache request context, where it's available for inspection by the Proconsul application, then passes the browser connection on to the Apache server.
5. The Apache server proxies the browser's request to the Proconsul Java servlet.
6. The Proconsul Java servlet inspects the request headers to retrieve user's identity information, and queries the Proconsul database to determine:
  - whether user is authorized to access Proconsul at all.  If user is not authorized, the application returns an error page.
  - whether user is authorized to connect to any target systems within Proconsul.  If user is not authorized for any target systems, the application returns an error page.
  - what target systems user is authorized to access via Proconsul (either as a normal user or as a Domain Admin user), and what, if any existing, disconnected sessions exist previously initiated by user.
7. The Proconsul Java servlet establishes a session with user's browser, and generates a randomized CSRF token which it stores in the Proconsul database for later validation, then constructs the personalized Proconsul UI page and delivers it to user's browser, along with the CSRF token and a session ID cookie.
8. User selects from the UI either an existing session to re-connect with or a new target system to connect to, and optionally specifies a display name and chooses a screen resolution for use with the session, then POSTs the UI's form back to the Proconsul server.
9. The Proconsul Java servlet receives the now-authenticated POST (along with the same SAML information it received in (5) above) and performs additional authorization checks against the Proconsul database, including:
  - whether the request includes a still-valid CSRF token associated with the session ID in the browser's cookie.  If no CSRF token is presented or the CSRF token is expired or not properly assocaited, the request is denied and the application returns an error page.
  - whether the user is still authorized to access Proconsul at all, returning an error if not
  - whether the user is authorized to access the chosen target host, returning an error if not
  - if the request is for a Domain Admin connection, whether the target host is authorized to receive Domain Admin connections, returning an error if not
  - if the request is for a Domain Admin connection, whether user is authorized to make Domain Admin connections, returning an error if not
10. Assuming authorization checks are successful, the Proconsul server retrieves additional data from the Proconsul database to determine:
  - what group(s) user's dynamic session user should be made a member of in the AD.  This may be determined based on user's unique identifier and/or other assertions provided in the active SAML response.
  - what group(s) may be required by the target system user is requesting access to
  - what group(s) may be required for access to any interposed RDP gateways
  - what POSIX attributes to assign to the dynamic session user based on the logged-in user's identity
  - what RDP gateway is required to access the target system (if any).
11. The Proconsul servlet then generates a randomized username and a randomized (64-character long) password, and (using a pooled LDAPs connection to one of the AD DCs) creates a user with the chosen sAMAccountName value and sets its password.  The servlet iterates through the configured AD domain controllers for the domain and/or site, waiting until each DC has received the user account.
12. For each group membership identified in (10), the servlet then repeats a similar process to (11) above, adding the dynamically-created user to each required group, and waiting to verify that all the relevant DCs receive the update before continuing.
13. The Proconsul servlet registers the session in the Proconsul database and identifies the first available X display / port number pair, registering it as "in use" in the database,  generates a randomized string to use as a VNC password, and instantiates a Docker container from the Docker image appropriate for the screen resolution requested by user, passing in the dynamic user information, port and display numbers and VNC password string as environment variables in the container context.
14. The servlet waits until the Docker container completes its startup processing (as signalled via the creation of a file in the container) and the redirects user's browser to a URL constructed from the base URI of the noVNC installation on the Proconsul server.  The URL includes arguments specifying the VNC password and the port number passed into the Docker container.
15. At the end of startup processing, the Docker container will have instantiated a websockify server listening on the specified external port for websocket connections, an XVnc server listening for traffic from the websockify proxy server, and an XfreeRDP client running in the X display of the XVnc server.  The XFreeRDP client will have connected to the target host (optionally using the RDP gateway identified in (10) above) and authenticated as the dynamic user created in (11) above.
16. User's browser downloads the noVNC JavaScript client, which uses the URL arguments to make a web socket connection to the Docker container listening on the specified port, authenticating to the VNC server there using the specified password.
17. At this point, user's browser is communicating over an SSL-wrapped websocket channel with a bespoke Docker container presenting a desktop session on the target system, authenticated as a dynamically created user with the POSIX attributes (uidnumber, gidnumber, homedirectory, etc.) configured for user, group memberships assigned based on policy in the Proconsul database, and a randomized username and random (unknown) password.  The user simply sees a desktop session presented inside his or her browser frame, and can operate normally in that session.
18. If user disconnects (or is disconnected for some reason) from the running session, the Docker container running the XFreeRDP client terminates.  The docker-gen daemon detects the termination, inspects the container for exit code and configuration information, and marks the user's session as "disconnected" in the Proconsul database after re-randomizing the dynamic session user's AD password.  User may subsequently return to the Proconsul application and select the disconnected session for reconnection, in which case the process repeats more or less as above (but instead of step (11) involving *creating* a new user account in the AD, it involves assigning a fresh random password to the existing user and re-applying group memberships as appropriate.
19.  If user logs out of the running session (or the remote server terminates the session due to memory limitations, etc.) the Docker container running the XFreeRDP client terminates with a different exit code.  The docker-gen daemon detects the terminal, inspects the container for the exit code, and in this case, deletes the dynamic session user from the AD domain, marking the session in the Proconsul database as terminated and marking the ports used by the session as free for re-use by another container.
