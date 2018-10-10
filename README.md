# Proconsul: Console Access for the Provinces

Proconsul is a tool for providing web-based access to Windows RDP
sessions.  It was designed to address a constellation of issues
identified at Duke University, some as part of routine security
reviews, and some as part of development efforts associated with a
grant from the National Science Foundation (cf. the Acknowledgments
file in this directory).  The key issues Proconsul was designed to
address include:

* Zero-install RDP client access.  Research collaborators who may need
  remote access to Windows workstations may not have nor want to
  install traditional RDP clients on their systems, and in some cases,
  may not have access to RDP clients nor the rights necessary to
  install them on their systems.  Proconsul provides a web-based
  interface for establishing RDP sessions with configured Windows
  systems that requires nothing but an HTML5-capable web browser.

* Federated RDP client access.  Traditional federated SSO using SAML
  (for instance, through the US InCommon federation) depends on the
  use of a web browser.  Traditional RDP clients are not equipped to
  support typical browser-based federated authentication processes,
  but research collaborators at federation partner sites frequently
  require access to Windows hosts at remote sites.  Proconsul provides
  a means to support federated access to remote desktop sessions by
  embedding RDP access in a browser window, and by managing the
  mapping of federated users onto dynamically-managed domain users at
  the target site.

* RDP client access in restricted environments.  Research artifacts of
  a sensitive nature may be required to remain within
  tightly-controlled network environments.  Proconsul supports the use
  of RDP gateways, and acts as a concentration point for incoming RDP
  connections, allowing an RDP gateway within a secured network to be
  accessed only from designated Proconsul concentrators outside the
  secured network, reducing the number and complexity of firewall
  rules required to enable authorized access.

* Reduction of Windows attack surfaces.  Numerous successful Active
  Directory domain compromises at R-1 institutions in the US in recent
  years have been traced back to a small number of APTs using similar
  attack vectors and similar MOs.  Proconsul provides a means to
  reduce the attack surfaces used by these APTs by:

    - Reducing the lifetime/viability of both unprivileged and
      privileged credentials.  When using Proconsul, both the username
      and password used to access the target Windows system are
      created at the time a session is initiated using random strings.
      Credentials are automatically destroyed at the termination of
      the user's Windows session, rendering them inert for purposes of
      later abuse.  If no Proconsul users are actively using domain
      admin privileges, no user-accessible domain admin accounts need
      exist in the AD.

    - Reducing the phishability of privileged AD credentials.  Since
      users of Proconsul are not aware of the credentials their
      sessions are using, and since Proconsul controls the privileges
      (group memberships) assigned to those sessions as a function of
      explicit privileges granted to specific federated users by
      Proconsul itself, Proconsul users cannot act as vectors for
      inadvertent disclosure of credentials.

    - Injecting non-Windows systems into traditionally Windows-only
      operations.  A number of successful APT compromises have taken
      advantage of various Windows vulnerabilities on administrators'
      *client* workstations in order to acquire NTLM or NTLMv2 hashes
      of privileged credentials, and of the Windows "pass the hash"
      mechanism to apply credential hashes sniffed from memory on
      vulnerable client system to remote operations on domain
      controllers, etc.  Proconsul replaces typical Widnows client ->
      Windows server connections with web browser -> Linux server ->
      Windows server connections, making this classic attack vector
      much more difficult to use.  Attackers must compromise the
      Linux-based Proconsul server in order to acces any privileged AD
      credentials.  Experience has shown that Linux servers can be
      more easily made resistent to this form of compromise than
      Windows workstations.

    - Enabling the use of federation authentication mechanisms,
      including multifactor mechanisms, to strengthen the
      authentication process.  Since Proconsul is a web-based
      application, it can partake of the full set of capabilities
      avaialble to federated relying parties, including various
      commercial and open source multifactor solutions.  While Windows
      itself can take advantage of multifactor authentication,
      implementing multifactor authentication *within* an Active
      Directory can be expensive and difficult compared to
      implementing multifactor authentication at a SAML IDP.

## Components

Proconsul consists of four separate components:

* The proconsul application itself -- a Spring-based Java web
  application.  Typically, Proconsul will be run in a Java container
  such as Tomcat or Jetty.  No specific container is provided with the
  software.  Development was done using a 1.7 OpenJDK JVM and Tomcat
  version 7, but any reasonably recent Java hosting environment should
  suffice.  Because we use Shibboleth for SAML authentication for the
  application, we run Tomcat behind an Apache server, which in turn is
  integrated with a Shibboleth SP. The application's souce code can be
  found in the "app" directory.

* noVNC -- an HTML5 and JavaScript implementation of the VNC client.
  noVNC is used to enable a browser client to interact with a VNC
  session running on the Proconsul server and hosting a Windows RDP
  session.  A customized version of the original noVNC code with minor
  adjustments to provide a somewhat better end-user experience is
  included in the "novnc" directory.

* A custom RDP client built as a Docker container.  The Proconsul
  server must support Docker.io's Linux container mechanism, and must
  be running a Docker server.  The Proconsul application starts an
  instance of the RDP client Docker container for each active user
  session.  Source for building your own RDP client container
  (complete with the source distribution of the FreeRDP client) is
  included in the "rdpstack-docker" directory.

* A custom docker-gen configuration.  As an adjunct to basic privilege
  separation, the RDP client Docker container does not have sufficient
  privileges to modify its own entries in the proconsul database, nor
  does it carry any privileged AD credentials other than those it uses
  to establish user sessions with remote systems.  This means that the
  container itself cannot address clean-up of accounts and database
  artifacts associated with it when its user session terminates.  For
  this, a custom docker-gen installation is used.  Docker-gen listens
  for updates from the running Docker server and performs clean-up
  functions for sessions upon termination of their associated Docker
  containers.  Docker-gen and the required custom configuration and
  scripts are included in the "docker-gen" directory.

## **NEW**:  Dockerized Installation

Proconsul now offers, in addition to the standard, manually installation process, a fully-
dockerized installation process.  The Dockerized installation process is significantly
less labor-intensive than the manual process, and is probably the quickest and simplest way
to get a Proconsul instance up and running in your AD environment.  It still requires 
some preparation of your AD domain, and some initial configuration, but the bulk of the 
build process for all the relevant Docker containers, the Proconsul and Proconsul Admin code,
and the set-up of the Java environment/web server environment is largely scripted.

If you're interested in using the Dockerized deployment, you'll want to move into the 
`Dockerized` diretory under this directory and review the `README.md` file there for 
instructions on setting up your AD, preparing your Docker server, and configuring the 
Dockerized build process.  

The current packaging of Proconsul for a fully Dockerized deployment assumes you will be 
using SAML to handle user authentication, and configures and deploys a Shibboleth SP as part
of the docker image build process. In future, the Dockerized build process may include additional authentication options, but for now, if you wish to authenticate users in a different way,you'll need to use the manual build process and make your own arrangements for authentication.

Unless you're planning to develop extensions to Proconsul itself or you need to use a non-
SAML authentication mechanism, it's strongly recommended that you use the Dockerized 
deployment rather than attempt a fully manual installation.  If you must perform a full
manual installation, the Installation instructions below may get you started in the right 
direction -- inspecting the build scripts under the Dockerized deployment tree should 
provide more insight into everything that's needed to successfully build and install the 
software manually.  The `README.md` file in the `Dockerized` directory will also provide
insight into the workings of the Dockerized installation, from which you may infer 
the bulk of what's required to perform a fully manual installation.

## Manual Installation

Full documentation for the manual installation of Proconsul at a new site has
yet to be developed, but at a very high level, the process involves a
few basic steps:

1. Build the Proconsul and Proconsul Admin applications from the source code provided.
   Both applications are now configured as Maven projects, so assuming you have a working
   Maven installation on your system, you should be able to cd into the proconsul or
   proconsuladmin directory and run `mvn install` to build the appropriate war file(s).

2. Establish a Java container server for the environment and deploy
   the application war files on the server.  Proconsul assumes
   that user authentication is being performed by the container or
   something logically "in front of" it. The source code does not
   handle end-user authentication, and simply expects a user
   identifier to be available as part of the HTTP request it receives.

3. Build and deploy the RDP client Docker container on your server,
   and edit the proconsul.properties configuration file for your
   container *and* your AD domain environment.  

4. Build and deploy docker-gen along with the custom docker-gen
   configuration included herein on your server.

5. Run the "proconsul.db.schema" SQL script on your MySQL server or
   (if you prefer to use another database provider) configure
   databases and tables according that that schema in your preferred
   database environment, and configure Proconsul to find the
   appropriate database.

6. Configure your AD with the groups, OUs, and users required by
   Proconsul.  For the moment, the best way to determine what those
   are is to review the Proconsul source code, or contact the authors
   for more information.

7. An administrative UI is now available as the "proconsuladmin" application.
   You can use it to configure the Proconsul database with explicit user->host,
   user->orgUnit, user->group, group->host, group->orgUnit, and 
   group->group mappings, as well as to configure POSIX attributes for specific 
   users and control access to both the application itself and the 
   Domain Admin functionality within it.  For instructions on using the 
   administrative interface, consult the README.md file in the `Dockerized`
   directory.

## Questions

Proconsul is provided as-is under the MIT License.  No warranty is
expressed or implied for any purpose.  Some components of the software
were developed with funding from the national Science Foundation---see
the Acknowledgements files in the various directories for details.
Some parts of the software are Copyright 2016 Duke University and are
distributed under the MIT License.  Other components are included from
other projects and are copyright their original authors and included
under their original licenses.

No explicit support is offered for Proconsul, but Proconsul is in use
at Duke University for a variety of purposes, and the software remains
under active development.  We're interested to know if you find
Proconsul useful, and will gladly accept bug reports as well as
feature requests (although no guarantee can be made as to
responsiveness at this time, as grant funding for the project runs out
in August 2016).

For questions and to report bugs, contact the author directly at the
address rob\<at\>duke.edu.
