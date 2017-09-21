package edu.duke.oit.idms.proconsul;

import java.io.File; 
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.sun.istack.internal.logging.Logger;

import edu.duke.oit.idms.proconsul.cfg.PCConfig;
import edu.duke.oit.idms.proconsul.util.ADUser;
import edu.duke.oit.idms.proconsul.util.AuthUser;
import edu.duke.oit.idms.proconsul.util.DockerContainer;
import edu.duke.oit.idms.proconsul.util.LDAPUtils;
import edu.duke.oit.idms.proconsul.util.ProconsulSession;
import edu.duke.oit.idms.proconsul.util.ProconsulUtils;

@Controller
public class ProconsulController {

	private static final Logger LOG = Logger.getLogger(ProconsulController.class);
	
	// Just for testing the deployment -- GET handler for /test
	@RequestMapping(value="/test", method=RequestMethod.GET)
	public ModelAndView handleTestRequest() {
		ModelAndView model = new ModelAndView("/test");
		model.addObject("testvalue",LDAPUtils.getOu("rob"));

		LOG.info("Returning model from /test endoint");
		return model;
	}
	
	// POST handler for /reconnect
	@RequestMapping(value="/reconnect", method=RequestMethod.POST)
	public ModelAndView handleReconnectPost(@ModelAttribute ReconnectRequest rr,HttpServletRequest request) {
		//ModelAndView model = new ModelAndView("/reconnectresult");
		//model.addObject("selected",rr.getTargetFQDN());
		//return model;
		// Handle a reconnect request.
		// We receive as input the reconnectrequest
		// Two possibilities -- reconnect or terminate
		// For now, we only handle reconnect -- terminate must wait until we have a 
		// means for remotely destroying the user session.
		//
		//
		// We additionally need to address the case in which the client did not disconnect but merely 
		// terminated its HTTP connection, leaving our Docker container active.  In that case, 
		// we need to simply redirect to the appropriate URL and re-establish connectivity to the 
		// VNC session (possibly at the expense of another running session, if the user has actually opened a 
		// new connection.
		
		if (rr == null || rr.getTargetFQDN() == null || rr.getTargetFQDN().equalsIgnoreCase("")) {
			//invalid request -- return the error page
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","No target system specified.");
			return errModel;
		}
		if (rr.getRequestType() != null && rr.getRequestType().equalsIgnoreCase("terminate")) {
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","Termination of disconnected sessions is not supported at this time -- try reconnecting and logging out to terminate the session.");
			return errModel;
		}

		// Collect the AuthUser
		AuthUser au = new AuthUser(request);
		LOG.info("Reconnect request received from " + au.getUid());
		
		if (! ProconsulUtils.canUseApp(au)) {
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","You ("+au.getUid()+") are not authorized to use this application.");
			return errModel;
		}
		LOG.info("Verified canUseApp for recon request");
		
		// CSRF protections
		if (! ProconsulUtils.checkOrigin(request)) {
			LOG.info("CSRF violation");
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","CSRF origin violation -- your browser is not authorized for this operation.");
			return errModel;
		}
		String token = rr.getCsrfToken();
		if (token != null) {
			String validToken = ProconsulUtils.getSessionCSRFToken(request);
			if (! token.equals(validToken)) {
				LOG.info("CSRF token violation");;
				ModelAndView errModel = new ModelAndView("/authzError");
				errModel.addObject("message","CSRF token violation -- your browser is not authorized for this operation.");
				return errModel;
			}
		} else {
			LOG.info("CSRF token violation -- no token present");
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","CSRF token violation -- your browser did not send the proper token to authorize this operation.");
			return errModel;
		}
		
		
		// Here, we implicitly restrict the session to those sessions available to the user.
		// Even if the user intentionally crafts a reconnect request for a session that isn't
		// his, we need to work properly -- in that case, by refusing authorization.
		//
		// Get the list of sessions the authuser has access to:
		ArrayList<ProconsulSession> psa = ProconsulUtils.getAvailableSessions(au); 
		// Find the appropriate session in the list.  If the session is not in the list, generate
		// an authorization error.  If the session is in the list, continue with it.
		ProconsulSession reconnectSession = null;
		
		for (ProconsulSession tsession : psa) {
			if (tsession.getOwner() != null && tsession.getOwner().getUid() != null && tsession.getOwner().getUid().equalsIgnoreCase(au.getUid()) && tsession.getFqdn() != null && tsession.getFqdn().equalsIgnoreCase(rr.getTargetFQDN())) {
				// tsession matches request -- run with it
				reconnectSession = tsession;
			} else {
				LOG.info("Failed match for session owned by " + tsession.getOwner() + " and target host " + tsession.getFqdn());
			}
		}
		// If we did not come out with a session, deny request
		if (reconnectSession == null) {
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","Your request cannot be processed -- either the session you are attempting to reconnect to does not exist or you are not authorized to it.");
			return errModel;
		}
		
		// Otherwise reconnectSession is our reconnect target
		// Wire up the session again and redirect to a connection to it.
		//
		// Here, we check for possible extand docker container and session
		// if (reconnectSession.getStatus().equals(Status.CONNECTED) || reconnectSession.getStatus().equals(Status.STARTING)) {
		if (false) {  // temporarily removed
			// This is a literal reconnect -- simply redirect
			String redirectUrl = reconnectSession.getRedirectUrl();
			return new ModelAndView("redirect:" + redirectUrl);
			
		} else {
			LOG.info("Reconnect starting new container due to session status " + reconnectSession.getStatus().toString());
		}
		reconnectSession.setNovncPort(ProconsulUtils.getVncPortNumber(reconnectSession));
		reconnectSession.setStatus(Status.STARTING);
		reconnectSession.setStartTime(new Date());
		// Reset the user password for the session
		reconnectSession.setTargetUser(ProconsulUtils.setRandomADPassword(reconnectSession.getTargetUser()));
		reconnectSession.setVncPassword(ProconsulUtils.getVncPassword());
		// Write updated session to database
		ProconsulUtils.writeSessionToDB(reconnectSession);
		LOG.info("Wrote updated reconnect session to database");
		// Assume the groups associated with the AD user have not been changed
		// Note:  This code will not change groups -- if something else does this
		// may need to be revisited.
		//
		// Spawn a docker container...
		DockerContainer container = new DockerContainer(reconnectSession);
		LOG.info("Created new container");
		if ((request.getParameter("rresolution") != null && request.getParameter("rresolution").equals("large")) || (request.getParameter("rdaresolution") != null && request.getParameter("rdaresolution").equals("large")))
			container.start("large");
		else
			container.start();
		LOG.info("Started container");
		reconnectSession.setStatus(Status.CONNECTED);
		ProconsulUtils.writeSessionToDB(reconnectSession);
		File fcheck = null;
		File dcheck = null;
		try {
			int pnum = reconnectSession.getNovncPort() + 5901;
			dcheck = new File("/var/spool/docker");
			if (dcheck.exists()) {
				fcheck = new File("/var/spool/docker/" + pnum);
				do {
					Thread.sleep(1000);
					System.out.println("File " + fcheck.getPath() + "still not there");
				} while (! fcheck.exists());
				Thread.sleep(1000);
			} else {
				LOG.info("Waiting another 5 seconds for spinup");;
				Thread.sleep(5000);
			}
		} catch (Exception e) {
			//ignore
		} finally {
			if (fcheck.exists()) {
				try {
					fcheck.delete();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
		LOG.info("Completed waiting loop");
		
		String redirectUrl = reconnectSession.getRedirectUrl();
		return new ModelAndView("redirect:" + redirectUrl);
	}
	// POST handler for /domainadmin
	@RequestMapping(value="/domainadmin", method=RequestMethod.POST)
	public ModelAndView handleDomainAdminPost(@ModelAttribute DomainAdminRequest domreq,HttpServletRequest request) {
		
		// For testing purposes, we're using a dummy group for DA access.
		String dagroup = ProconsulUtils.getADDomainAdminGroupDn();
		
		String targetFQDN = domreq.getTargetFQDN();
		// Limit characters in targetFQDN to defeat XSS
		targetFQDN = targetFQDN.replaceAll("[^a-zA-Z0-9._-]", ""); // xss protection
		// Not supporting password exposure yet
		//boolean passwordExposure = domreq.isExposePassword();  // Not currently supported
		String displayName = domreq.getDisplayName();
		// XSS avoidance
		displayName = displayName.replaceAll("[^A-za-z0-9:,_@?! -]", "");
		
		// Check authorization -- this is serious
		
		AuthUser au = new AuthUser(request);
		LOG.info("Received domain admin login request from " + au.getUid());
		
		if (! ProconsulUtils.canUseApp(au)) {
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","You (" + au.getUid()+") are not authorized to use this application");
			LOG.info("User " + au.getUid() + " attempt to use Proconsul failed due to authz error");
			return errModel;
		}
		LOG.info("Verified canUseApp");
		
		if (! ProconsulUtils.canUseDA(au)) {
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","You are not a domain admin -- this will be reported");
			LOG.info("User " + au.getUid() + " attempt to use Proconsul for domain admin access failed to to authz error");
			return errModel;
		}
		
		// CSRF protections
		if (! ProconsulUtils.checkOrigin(request)) {
			LOG.info("CSRF violation");
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","CSRF origin violation -- your browser is not authorized for this operation.");
			return errModel;
		}
		String token = domreq.getCsrfToken();
		if (token != null) {
			String validToken = ProconsulUtils.getSessionCSRFToken(request);
			if (! token.equals(validToken)) {
				LOG.info("CSRF token violation");;
				ModelAndView errModel = new ModelAndView("/authzError");
				errModel.addObject("message","CSRF token violation -- your browser is not authorized for this operation");
				return errModel;
			}
		} else {
			LOG.info("CSRF token violation -- no token presented");
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","CSRF token violation -- your browser did not present the required token for this operation");
			return errModel;
		}

		// If we are authorized to use the app and we are authorized to use the feature
		// we continue from here
		
		ArrayList<String> dafqdns = ProconsulUtils.getDomainAdminFQDNs();
		if (! dafqdns.contains(targetFQDN)) {
			// Unauthorized host requested
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","You may only access hosts authorized by the system for domain admin access with this feature -- access denied");
			LOG.info("User " + au.getUid() + " attempt to make DA connection to " + targetFQDN + " denied because host is not authorized for DA access");
			return errModel;
		}
		
		// Now we know that the host is OK and the user is OK -- proceed
		
		String vncPassword = ProconsulUtils.getVncPassword();
		LOG.info("Created new vnc password");
		ADUser targetUser = ProconsulUtils.createRandomizedADUser();
		LOG.info("Created new target AD user");
		ProconsulSession session = new ProconsulSession(au,targetFQDN,vncPassword,targetUser,displayName,"domain");
		LOG.info("Created new session");
		
		// Now we pick a port for the session
		session.setNovncPort(ProconsulUtils.getVncPortNumber(session));
		session.setStatus(Status.STARTING);  // Make session starting in the database
		session.setStartTime(new Date());
		
		// And we add a gateway if needed
		session = ProconsulUtils.addGatewayToSession(session);
		
		ProconsulUtils.writeSessionToDB(session);
		
		// At this point, we need to make some adjustments to the user's group memberships.
		// User must become a member of the registered DA group
		// First, put the user into the authorized list, though
		if (! ProconsulUtils.makeUserAuthorizedDA(targetUser)) {
			//
			// In this case, we're not able to set the validation for the user, hence we cannot continue
			// Since at this point the user exists but the docker container does not, we cannot 
			// rely on docker-gen to handle our cleanup.  We must remove the object ourselves.
			//
			ProconsulUtils.deleteAdUser(targetUser);  // delete the user
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","Unable to update authorization to allow DA access - try again later");
			LOG.info("Failed to update proconsul database with new AD admin -- user " + au.getUid() + " failed to log in");
			return errModel;
		}
		// Otherwise, the user is now in the active_domain_admins list -- we can add
		// to the relevant group
		
		ADUser updated = ProconsulUtils.addGroupToADUser(dagroup, targetUser);
		if (updated == null) {
			// Failure of the add
			ProconsulUtils.deleteAdUser(targetUser);
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","Unable to update authorization to allow DA access - try again later");
			LOG.info("Failed to update protocol database with new AD admin -- user " + au.getUid() + " failed to log in");
			return errModel;
		}
		
		// And in case there is a gateway involved, add its group as well (since DA may not be sufficient)
		updated = ProconsulUtils.addGatewayAccessGroup(updated, targetFQDN);
		
		// Otherwise, user is now in the specified DA group
		
		session.setTargetUser(updated);
		ProconsulUtils.writeSessionToDB(session);
		
		DockerContainer container = new DockerContainer(session);
		LOG.info("Created new container");
		if (request.getParameter("daresolution") != null && request.getParameter("daresolution").equals("large")) 
			container.start("large");
		else
			container.start();
		LOG.info("Started new container");
		
		session.setStatus(Status.CONNECTED);
		ProconsulUtils.writeSessionToDB(session);
		
		File dcheck = null;
		File fcheck = null;
		try {
			int pnum = session.getNovncPort() + 5901;
			dcheck = new File("/var/spool/docker");
			if (dcheck.exists()) {
				fcheck = new File("/var/spool/docker/" + pnum);
				do {
					Thread.sleep(1000);
					System.out.println("File " + fcheck.getPath() + " still not there");
				} while (! fcheck.exists());
				
				Thread.sleep(1000);
			} else {
				Thread.sleep(5000);
			}
		} catch (Exception e) {
			// ignore
		} finally {
			if (fcheck.exists()) {
				try {
					fcheck.delete();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
		
		String redirectUrl = session.getRedirectUrl();
		return new ModelAndView("redirect:" + redirectUrl);
	}
	// POST handler for /deladmin
	@RequestMapping(value="/deladmin", method=RequestMethod.POST)
	public ModelAndView handleDelegatedAdminPost(@ModelAttribute DelegatedAdminRequest deladmin, HttpServletRequest request) {
	
		String targetFQDN = deladmin.getHostName();
		// XSS defeat
		targetFQDN = targetFQDN.replaceAll("[^A-Za-z0-9._-]", ""); // xss protection
		String displayName = deladmin.getDisplayName();
		displayName = displayName.replaceAll("[^A-za-z0-9:,_@?! -]", "");

		String orgUnit = deladmin.getOrgUnit();
		String roleGroup = deladmin.getRoleGroup();
		
		// Check authorization -- this is significant -- can't be too careful here
		
		AuthUser au = new AuthUser(request);
		LOG.info("Received delegated admin login request from " + au.getUid());
		
		if (! ProconsulUtils.canUseApp(au)) {
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","You (" + au.getUid()+") are not authorized to use this application");
			LOG.info("User " + au.getUid() + " attempt to use Proconsul for delegated admin failed due to authz error");
			return errModel;
		}
		LOG.info("Verified canUseApp");
		
		if (! ProconsulUtils.canAdminFQDN(au,targetFQDN)) {
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","You are not auhtorized to administer systems and data in this OU -- this will be reported");
			LOG.info("User " + au.getUid() + " attempt to use Proconsul for delegated admin failed due to authz error");
			return errModel;
		}
		
		// Also check if any requested group is allowable
		ArrayList<String> allowableGroups = ProconsulUtils.groupDNsForDelegate(au);
		boolean groupOK = false;
		for (String allowed : allowableGroups) {
			if (roleGroup == null || roleGroup.equalsIgnoreCase(allowed)) {
				groupOK = true;
			}
		}
		if (! groupOK) {
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","You are not authorized to claim the role group " + roleGroup + " -- this will be reported");
			LOG.info("User " + au.getUid() + " failed attempt to claim role group " + roleGroup);
			return errModel;
		}
		
		// CSRF protections
		if (! ProconsulUtils.checkOrigin(request)) {
			LOG.info("CSRF violation");
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","CSRF origin violation -- your browser is not authorized for this operation.");
			return errModel;
		}
		String token = deladmin.getCsrfToken();
		if (token != null) {
			String validToken = ProconsulUtils.getSessionCSRFToken(request);
			if (! token.equals(validToken)) {
				LOG.info("CSRF token violation");;
				ModelAndView errModel = new ModelAndView("/authzError");
				errModel.addObject("message","CSRF token violation -- your browser is not authorized for this operation.");
				return errModel;
			}
		} else {
			LOG.info("CSRF token violation -- no token presented");
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","CSRF token violation - your browesr did not provide the required token for this operation");
			return errModel;
		}
		
		//At this point we're an authorized user authorized to access the target host.  
		
		String vncPassword = ProconsulUtils.getVncPassword();
		LOG.info("Created new vnc password");
		ADUser targetUser = ProconsulUtils.createRandomizedADUser();
		LOG.info("Created new target AD user");
		ProconsulSession session = new ProconsulSession(au,targetFQDN,vncPassword,targetUser,displayName,"delegated",orgUnit,roleGroup);
		LOG.info("Created new session");;
		
		session.setNovncPort(ProconsulUtils.getVncPortNumber(session));
		session.setStatus(Status.STARTING);
		session.setStartTime(new Date());
		
		// Add a gateway if required by the host
		session = ProconsulUtils.addGatewayToSession(session);
		
		ProconsulUtils.writeSessionToDB(session);
		
		// Adjust groups for the projected user to match what's required for the delegate
		//
		// Start with the default group (for internal tracking and global access)
		PCConfig config = PCConfig.getInstance();
		String defgroupdn = config.getProperty("ldap.defgroupdn", true);
		ADUser defaulted = ProconsulUtils.addGroupToADUser(defgroupdn, targetUser);
		// Add the host access group (for RDP access) if there is one for FQDN
		ADUser updated = ProconsulUtils.addHostAccessGroup(defaulted, targetFQDN);
		if (updated == null) {
			ProconsulUtils.deleteAdUser(targetUser);
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","Unable to add group membership for RDP access -- try again later");
			LOG.info("Failed to add target host group for " + au.getUid() + " failing login");
			return errModel;
		}
		// Add the administrator group (lowest priv) for the FQDN if there is one
		ADUser updated2 = ProconsulUtils.addGroupToADUser(ProconsulUtils.groupDnForFQDN(targetFQDN), updated);
		if (updated2 == null) {
			ProconsulUtils.deleteAdUser(updated);
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","Unable to add group membership for delegated admin -- try again later");
			LOG.info("Failed to add delegated admin group for " + au.getUid() + " failing login");
			return errModel;
		}
		// Add any passed-in authorized delegated admin group as well, just in case
		ADUser updated3 = ProconsulUtils.addGroupToADUser(roleGroup, updated2);
		if (updated3 == null) {
			ProconsulUtils.deleteAdUser(updated2);
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","Unable to add requested group membership for delegated admin -- try gain later");
			LOG.info("Failed to add requested delegate groupf or " + au.getUid() + " -- " + roleGroup + " -- failing login");
			return errModel;
		}
		
		// If a gateway is being used, add the gateway access group as well
		updated3 = ProconsulUtils.addGatewayAccessGroup(updated3, targetFQDN);
		
		// Otherwise, we have a prepared user in updated3
		
		session.setTargetUser(updated3);
		ProconsulUtils.writeSessionToDB(session);
		
		DockerContainer container = new DockerContainer(session);
		LOG.info("Created new container");;

		container.start();
		LOG.info("Started new container");
		
		session.setStatus(Status.CONNECTED);
		ProconsulUtils.writeSessionToDB(session);

		File dcheck = null;
		File fcheck = null;
		try {
			int pnum = session.getNovncPort() + 5901;
			dcheck = new File("/var/spool/docker");
			if (dcheck.exists()) {
				fcheck = new File("/var/spool/docker/" + pnum);
				do {
					Thread.sleep(1000);
					System.out.println("File " + fcheck.getPath() + " still not there");;
				} while (! fcheck.exists());
				
				Thread.sleep(1000);
			} else {
				Thread.sleep(5000);
			}
		} catch (Exception e) {
			//ignore
		} finally {
			if (fcheck.exists()) {
				try {
					fcheck.delete();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
		
		String redirectUrl = session.getRedirectUrl();
		return new ModelAndView("redirect:" + redirectUrl);
	}
		
	//POST handler for /usersession
	@RequestMapping(value="/usersession",method=RequestMethod.POST)
	public ModelAndView handleUserSessionPost(@ModelAttribute UserSessionRequest usersession, HttpServletRequest request) {
		
		
		// Now we must handle the actual work of setting up the container and the session
		// We establish the session then pass a redirect URL into the result JSP
		//
		
		AuthUser au = new AuthUser(request);  // generate authuser from request
		LOG.info("Created authuser");
		LOG.info("authuser uid is" + au.getUid());
		// Verify authorization for AuthUser before proceeding
		// First verify app authorization 
		if (! ProconsulUtils.canUseApp(au)) {
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","You ("+au.getUid()+") are not authorized to use this application.");
			return errModel;
		}
		LOG.info("Verified canUseApp");
		// Now verify host authorization (cannot be too careful)
		//
		//
		if (au.getUid() == null) {
			// if we don't know who you are, you are persona non grata -- should be impossible, but...
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","Anonymous access is not allowed");
			return errModel;
		}
		if (! au.getUid().equals("") && ! ProconsulUtils.fqdnsForEppn(au.getUid()).contains(usersession.getTargetFQDN())) {
			// We don't have an explicit allow, but we might have an implicit group allow
			// Check the user's group URNs for access to the host before failing
			boolean groupAllow = false;
			for (String gurn : au.getMemberships()) {
				if (ProconsulUtils.fqdnsForGroupUrn(gurn).contains(usersession.getTargetFQDN())) {
					groupAllow = true;
					break;
				}
			}
			// If the user is not authorized to this FQDN, it's a spoof and we fail
			if (!groupAllow) {
				ModelAndView errModel = new ModelAndView("/authzError");
				errModel.addObject("message","You ("+au.getUid()+") are not authorized to use " + usersession.getTargetFQDN().replaceAll("[^A-Za-z0-9._-]", ""));
				return errModel;
			}
		}
		// At this point, either the user has explicit authorization or the user has group authorization 
		LOG.info("Verified host authorization");
		
		// CSRF protections
		if (! ProconsulUtils.checkOrigin(request)) {
			LOG.info("CSRF violation");
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","CSRF origin violation -- your browser is not authorized for this operation.");
			return errModel;
		}
		String token = usersession.getCsrfToken();
		if (token != null) {
			String validToken = ProconsulUtils.getSessionCSRFToken(request);
			if (! token.equals(validToken)) {
				LOG.info("CSRF token violation");;
				ModelAndView errModel = new ModelAndView("/authzError");
				errModel.addObject("message","CSRF token violation -- your browser is not authorized for this operation.");
				return errModel;
			}
		} else {
			LOG.info("CSRF token violation - no token presented");
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","CSRF token violation - your browser did not provide the required token for this operation.");
			return errModel;
		}
		
		//If we're here, we made it past basic authZ checks -- we can proceed.
		//
		// Create a new session to represent what's being requested.
		
		String vncPassword = ProconsulUtils.getVncPassword();
		LOG.info("Created new vnc password");
		ADUser targetUser = ProconsulUtils.createRandomizedADUser();
		LOG.info("Created new AD targetuser");
		String displayName = usersession.getDisplayName();
		displayName = displayName.replaceAll("[^A-za-z0-9:,_@?! -]", "");
		String targetFQDN = usersession.getTargetFQDN();
		targetFQDN = targetFQDN.replaceAll("[^A-za-z0-9._-]", "");
		ProconsulSession session = new ProconsulSession(au,targetFQDN,vncPassword,targetUser,displayName);
		LOG.info("Created new session");
		
		// Pick a port
		session.setNovncPort(ProconsulUtils.getVncPortNumber(session));
		// Set status to Starting
		session.setStatus(Status.STARTING);
		// Set session start time (for bookkeeping)
		session.setStartTime(new Date());
		
		// Add the gateway if a gateway is required
		session = ProconsulUtils.addGatewayToSession(session);
		
		// Write out the session to the database and encumber the port
		//
		ProconsulUtils.writeSessionToDB(session); // After this, we're committed
		LOG.info("Wrote new session to db");
		
		// Add the AD user to appropriate groups for the session.
		//
		// Since this is a user session, the only groups are those the authuser has 
		// as explicit group assignments and the default group
		// Start with the default group
		
		PCConfig config = PCConfig.getInstance();
		String defgroupdn = config.getProperty("ldap.defgroupdn", true);
		targetUser = ProconsulUtils.addGroupToADUser(defgroupdn, targetUser);
		LOG.info("Added user to default group");
		targetUser = ProconsulUtils.addHostAccessGroup(targetUser, usersession.getTargetFQDN());
		LOG.info("Handled FQDN access group");
		
		// And now any other groups required
		for (String dn : ProconsulUtils.groupDnsForEppn(au.getUid())) {
			LOG.info("Adding group " + dn + " to AD user for " + au.getUid());
			targetUser = ProconsulUtils.addGroupToADUser(dn, targetUser);
		}
		
		// And if a gateway is in use, add its access group
		targetUser = ProconsulUtils.addGatewayAccessGroup(targetUser, usersession.getTargetFQDN());
		LOG.info("Finished with group adds (possibly no groups found)");
		
		
		//And update the session database with new groups from targetUser
		session.setTargetUser(targetUser);
		// and re-write
		ProconsulUtils.writeSessionToDB(session);
		LOG.info("Wrote session second time");
		
		// Add restrictions on the user to limit logon access to just what is required
		// Apparently, this may not work with source IPs which are not joined workstations?
		// Commented out for now...
		// Also runs into will-not-perform failures from the AD when adding the values...
		// ProconsulUtils.addWorkstationsToADUser(session,targetUser);
		
		// Now we spawn a docker container based on the session
		DockerContainer container = new DockerContainer(session);
		LOG.info("Created new container");
		if (request.getParameter("resolution") != null && request.getParameter("resolution").equals("large"))
			container.start("large");
		else
			container.start();
		LOG.info("Started new container");
		// and update database with new status
		session.setStatus(Status.CONNECTED); // now we're connected
		ProconsulUtils.writeSessionToDB(session);
		LOG.info("Wrote session out final time");
		
		// And finally send back the redirect document to the user
		//
		// There's a secondary race condition here in which the websockify process
		// hasn't quite started yet and we lose on connection from the browser.
		// To combat that, pend until the websockify is ready by watching for
		// /var/spool/docker/ext-portnumber
		// TODO:  Replace this with the actual pend when we move to the production server
		// TODO:  For now, just wait ten seconds for the container to start properly
		// TODO:  Split the difference for now -- if there's a /var/spool/docker dir,
		// TODO:  wait for the file, else sleep and hope 
		//
		File fcheck = null;
		File dcheck = null;
		try {
			int pnum = session.getNovncPort() + 5901;
			dcheck = new File("/var/spool/docker");
			if (dcheck.exists()) {
				fcheck = new File("/var/spool/docker/" + pnum);
				do {
					Thread.sleep(1000);;
					System.out.println("File " + fcheck.getPath() + " still not there");
				} while (! fcheck.exists());
				Thread.sleep(1000);  // extra second just in case
			} else {
				// TODO:  For now, just wait
				LOG.info("Sleeping for 5 seconds");
				Thread.sleep(5000);
			}
		} catch (Exception ign) {
			//ignore
		} finally {
			if (fcheck.exists()) {
				try {
					fcheck.delete();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
		LOG.info("Wait complete");
		
		String redirectUrl = session.getRedirectUrl();
		LOG.info("Sending redirect URL");
		return new ModelAndView("redirect:" + redirectUrl);
	
	}
			
	// GET handler for /fqdn/{ou} to handle Ajax request for list of fqdn values
	// associated with a given OU
	// 
	@RequestMapping(value="/fqdn/{ou}", method=RequestMethod.GET)
	@ResponseBody
	public OuHostList handleFQDNOU(@PathVariable("ou") String ou) {
		LOG.info("Called handleFQDNOU()");
		OuHostList retval = new OuHostList();
		LOG.info("Entering fqdnsForOu("+ou+")");
		ArrayList<String> fqdns = ProconsulUtils.fqdnsForOu(ou);
		LOG.info("Exited fqdnsForOu with " + fqdns.size() + " entries");
		Collections.sort(fqdns);
		retval.setHosts(fqdns);
		return retval;
	}
	
		
	/*
	 * Handle root GET request.
	 * Uses main page view which contains four possible forms
	 *     Reconnect for existing sessions (POSTs to /reconnect)
	 *     DomainAdmin for available DomainAdmin rights (POSTs to /domainadmin)
	 *     DelegatedAdmin for available DelegateAdmin rights (POSTs to /delegatedadmin)
	 *     UserSession for available User rights sessions (POSTs to /normal)
	 *     
	 * Each form posts to a different endpoint to allow for overloading of fields names
	 * and simplify separation of handling behaviors (although it makes 
	 * for additional code).
	 *      
	 */
	@RequestMapping(value="/", method=RequestMethod.GET)
	public ModelAndView handleRootRequest(HttpServletRequest request) {
		//ModelAndView model = new ModelAndView("/main");
		ModelAndView model = new ModelAndView("/mary");
		
		// Apply CSRF protections to the page for later validation
		String csrfToken = ProconsulUtils.getSessionCSRFToken(request);
		if (csrfToken == null) {
			LOG.info("CSRF token creation failure");;
			ModelAndView errModel = new ModelAndView("/authzError");
			errModel.addObject("message","CSRF failure -- unable to establish secure session.  Please try agai later.");
			return errModel;
		} 
		ReconnectRequest recon = new ReconnectRequest();
		recon.setCsrfToken(csrfToken);
		model.addObject("recon",recon);
		ReconnectRequest userRecon = new ReconnectRequest();
		userRecon.setCsrfToken(csrfToken);
		model.addObject("userRecon",userRecon);
		ReconnectRequest delegatedRecon = new ReconnectRequest();
		delegatedRecon.setCsrfToken(csrfToken);
		model.addObject("delegatedRecon",delegatedRecon);
		ReconnectRequest domainRecon = new ReconnectRequest();
		domainRecon.setCsrfToken(csrfToken);
		model.addObject("domainRecon",domainRecon);
		DomainAdminRequest domadmin = new DomainAdminRequest();
		domadmin.setCsrfToken(csrfToken);
		model.addObject("domadmin",domadmin);
		DelegatedAdminRequest deladmin = new DelegatedAdminRequest();
		deladmin.setCsrfToken(csrfToken);
		model.addObject("deladmin",deladmin);
		UserSessionRequest usersession = new UserSessionRequest();
		usersession.setCsrfToken(csrfToken);
		model.addObject("usersession", usersession);
		
		AuthUser testUser = new AuthUser(request);
		// RGC - override for testing...testUser.setUid("rob@duke.edu");

		LOG.info("Root GET request received");
		
		// Check for authorization to even use the tool
		if (! ProconsulUtils.canUseApp(testUser)) {
			ModelAndView errModel = new ModelAndView("/authzError");
			PCConfig config = PCConfig.getInstance();
			errModel.addObject("logouturl",config.getProperty("logout.url", true));
			errModel.addObject("message","You are not authorized to use this tool.");
			LOG.info("User " + testUser.getUid() + " is not authorized to use Proconsul");
			return errModel;
		}
		
		// CSRF isn't necessary on GET in this case
		
		// Inform the UI who it's dealing with
		model.addObject("authenticatedUser",testUser);
		
		// Inform the UI about available sessions
		LOG.info("Searching for connectable sessions");
		// cheap to retrieve, so might as well do it separately for each case
		ArrayList<DisplayFQDN> hosts = ProconsulUtils.getAvailableSessionDisplayFQDNs(testUser,"");
		ArrayList<DisplayFQDN> userHosts = ProconsulUtils.getAvailableSessionDisplayFQDNs(testUser,"user");
		ArrayList<DisplayFQDN> delegatedHosts = ProconsulUtils.getAvailableSessionDisplayFQDNs(testUser, "delegated");
		ArrayList<DisplayFQDN> domainHosts = ProconsulUtils.getAvailableSessionDisplayFQDNs(testUser, "domain");
		
		model.addObject("sessionlist",hosts);
		model.addObject("userSessionList",userHosts);
		model.addObject("delegatedSessionList",delegatedHosts);
		model.addObject("domainSessionList",domainHosts);
		
		if (hosts.isEmpty()) {
			model.addObject("resumeclass","hiddendiv");
		} else {
			model.addObject("resumeclass","displaydiv");
		}
		
		if (userHosts.isEmpty()) {
			model.addObject("userResumeClass","hiddendiv");
		} else {
			model.addObject("userResumeClass","displaydiv");
		}
		if (delegatedHosts.isEmpty()) {
			model.addObject("delegatedResumeClass","hiddendiv");
		} else {
			model.addObject("delegatedResumeClass","displaydiv");
		}
		if (domainHosts.isEmpty()) {
			model.addObject("domainResumeClass","hiddendiv");
		} else {
			model.addObject("domainResumeClasss","displaydiv");
		}
		
		// Inform the UI about available domain admin hosts
		ArrayList<String> domhosts = ProconsulUtils.getDomainAdminFQDNs();
		model.addObject("domainhosts",domhosts);
		if (ProconsulUtils.canUseDA(testUser)) {
			model.addObject("daclass","displaydiv");
		} else {
			model.addObject("daclass","hiddendiv");
		}
		
		// Inform the UI about delegated admin roles the user may have and what they support
		ArrayList<String> groups = ProconsulUtils.groupDNsForDelegate(testUser);
		ArrayList<DisplayGroup> rolegroups = null;
		if (groups == null || groups.isEmpty()) {
			LOG.info("Sending empty rolegroups to main page due to null return from groupDNsforDelegate on " + testUser.getUid());
			rolegroups = new ArrayList<DisplayGroup>();  // empty for null here
		} else {
			rolegroups = new ArrayList<DisplayGroup>();
			for (String rg : groups) {
				LOG.info("Sending rolegroup value " + rg);
				rolegroups.add(new DisplayGroup(rg));
			}
		}
		model.addObject("roleGroups",rolegroups);
		ArrayList<DisplayOu> delegatedous = new ArrayList<DisplayOu>();
		for (String ou : ProconsulUtils.delegatedOusForUser(testUser)) {
			delegatedous.add(new DisplayOu(ou.replaceAll(",.*", "").replaceAll(".*=", ""),ou));
		}
		model.addObject("delegatedOUs",delegatedous);
		
		if (delegatedous.isEmpty()) {
			model.addObject("delegatedclass","hiddendiv");
		} else {
			model.addObject("delegatedclass","displaydiv");
		}
		
		ArrayList<String> userhosts = new ArrayList<String>();
		userhosts.addAll(ProconsulUtils.fqdnsForEppn(testUser.getUid()));
		LOG.info("After adding eppn hosts, now at " + userhosts.size());
		//userhosts.addAll(ProconsulUtils.fqdnsForGroupUrn("urn:mace:duke.edu:groups:fictional:sdn"));
		for (String m1 : testUser.getMemberships()) {
			userhosts.addAll(ProconsulUtils.fqdnsForGroupUrn(m1));
		}
		LOG.info("After adding group hosts, now at " + userhosts.size());
		//userhosts.addAll(ProconsulUtils.fqdnsForEntitlement("OKAY_FOR_DUKE"));
		for (String e1 : testUser.getEntitlements()) {
			userhosts.addAll(ProconsulUtils.fqdnsForEntitlement(e1));
		}
		LOG.info("After adding entitlement hosts, now at " + userhosts.size());
		model.addObject("userhosts",userhosts);
		if (userhosts.isEmpty()) {
			model.addObject("userclass","hiddendiv");
		} else {
			model.addObject("userclass","displaydiv");
		}
		if (userhosts.isEmpty() && delegatedous.isEmpty() && ! ProconsulUtils.canUseDA(testUser) && hosts.isEmpty()) {
			model.addObject("errMessage","You are not authorized to establish or reconnect to any sessions at this time");
		}
		
		PCConfig config = PCConfig.getInstance();
		model.addObject("logouturl",config.getProperty("logout.url", true));
		return model;
	}
}
