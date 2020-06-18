package edu.duke.oit.idms.proconsul.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;

//import com.sun.istack.internal.logging.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.duke.oit.idms.proconsul.CheckedOutCred;
import edu.duke.oit.idms.proconsul.DisplayFQDN;
import edu.duke.oit.idms.proconsul.Status;
import edu.duke.oit.idms.proconsul.cfg.PCConfig;

import java.util.Date;
import java.text.SimpleDateFormat;


public class ProconsulUtils {

	// Static class for utilities
	
	private static final Logger LOG = LoggerFactory.getLogger(ProconsulUtils.class);
	
	// Wrapper routines for generating log entries using loggers from other classes.
	//
	// We supply four classes of log:
	//
	//	audit:  Logged at the ERROR level *AND* written out to the audit trail DB table
	//  error:  Logged at the ERROR level but not reported into the audit trail
	//  log:    Logged at the INFO level as an informational log
	//  debug:  Logged at the DEBUG level as an informational log
	//
	// Audits and errors are always logged.  Log and debug messages are controllable with 
	// configuration (which by default suppresses them).
	//
	// For each of the utility routines, we take a string to log along with a Logger object to
	// log through.
	//
	//
	// The audit routine is more complex than the rest, owing to its needing additional tagged
	// information to populate into the audit trail (where appropriate)
	
	public static void audit(Logger logger, String event, String authUser, String targetHost, String targetUser, String targetGroup, String clientIp) {
		// First, write out the audit trail entry.  This is of prime importance.
		
		ProconsulUtils.writeAuditLog(authUser, event, targetUser, targetHost, targetGroup, clientIp);
		
		// Regardless, perform a log operation with the same information
		//
		logger.error(event + "(user=" + authUser + ",targethost="+targetHost+",targetuser="+targetUser+",targetgroup="+targetGroup+",clientip="+clientIp+")");
		
	}
	
	public static void error(Logger logger, String event) {
		// Write out the error log entry
		logger.error(event);
	}
	
	public static void log(Logger logger, String event) {
		// If the log level is >= INFO, write the log entry
		PCConfig config = PCConfig.getInstance();
		String loglevel = config.getProperty("loglevel", false);
		if (loglevel != null && (loglevel.equalsIgnoreCase("info") || loglevel.equalsIgnoreCase("debug"))) {
			logger.info(event);
		}
	}
	
	public static void debug(Logger logger, String event) {
		// if the log level is DEBUG write the log entry
		PCConfig config = PCConfig.getInstance();
		String loglevel = config.getProperty("loglevel",  false);
		if (loglevel != null && loglevel.equalsIgnoreCase("debug")) {
			logger.debug(event);
		}
	}
	
	public static boolean isSamaccountnameAvailable(String sam, LDAPAdminConnection lac) {
		DirContext dc = null;
		NamingEnumeration<SearchResult> result = null;
		try {
			dc = lac.getConnection();
			if (dc == null) {
				debug(LOG,"DC value is null in isSamaccountnameAvailable");
			}
			if (sam == null) {
				debug(LOG,"SAM is null in isSamaccountnameAvailable");
			}
			if (sam.equals("")) {
				debug(LOG,"SAM is empty but not null in isSamaccountnameAvailable");
			}
			if (dc == null || sam == null || sam.equals("")) {
				error(LOG,"Unable to check sAMAccountName availability for sAMAccountName=" + sam);
				return true;  //for specific server, "true" is "fail"
			}
			SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			PCConfig config = PCConfig.getInstance();
			
			
			String searchBase = config.getProperty("ldap.searchbase", true);
			
			result = dc.search(searchBase,  "samaccountname="+sam,sc);
			if (result == null || ! result.hasMore()) {
				return true;
			} else {
				return false;
			}
			
		} catch (Exception e) {
			throw new RuntimeException("Failed looking up samaccountname in AD -" + e.getMessage());
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (dc != null) {
				try {
					dc.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}
	
	public static boolean isSamaccountnameAvailable(String sam) throws NamingException {
		NamingEnumeration<SearchResult> results = null;
		DirContext dc= null;
		try {
			dc = LDAPConnectionFactory.getAdminConnection();
			if (dc == null || sam == null || sam.equals("")) {
				return false;  // fail closed
			}
			SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			
			PCConfig config = PCConfig.getInstance();
			
			String searchBase = config.getProperty("ldap.searchbase", true);
			
			results = dc.search(searchBase,  "samaccountname="+sam,sc);
			if (results == null) {
				debug(LOG,"Returning true becasue of null results return");
				return true;
			}
			if (results == null || ! results.hasMore()) {
				debug(LOG,"Returning true from isSamaccountnameavailable()");
				return true;
			} else {
				debug(LOG,"Returning false from isSamaccountnameavailable()");
				return false;
			}
		} catch (PartialResultException p) {
			// ignore partial result exceptions since we do not follow referrals in AD
			if (results == null || ! results.hasMoreElements()) {
				debug(LOG,"Partial result returning true from isSamaccountnameavailable()");
				return true;
			} else {
				debug(LOG,"Partial result returning false from isSamaccountnameavaialble() for " + sam);
				return false;
			}
		} catch (NamingException e) {
			error(LOG,"Throwing exception from isSamaccountnameavailable()");
			throw new RuntimeException("Failed looking up samaccountname in AD: " + e.getMessage() + " - throwable " + e.toString() + ":");
		} finally {
			if (results != null) {
				try {
					results.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (dc != null) {
				try {
					dc.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}
	
	// Static routine to find an unused random sAMAccountName value (lower-case only, for POSIX)
	public static String getRandomSamaccountname() throws NamingException {
		String retval = RandomStringUtils.randomAlphanumeric(9).toLowerCase() + "-eas";
		int count = 0;
		while (! ProconsulUtils.isSamaccountnameAvailable(retval) && count++ < 10) {
			// 10 retries before failing
			retval = RandomStringUtils.randomAlphanumeric(9).toLowerCase();
			retval += "-eas";
		}
		if (count < 10) {
			log(LOG,"Random available samaccountname is " + retval);
			return retval;
		} else {
			error(LOG,"Unable to find unused sAMAccountName value after 10 tries");
			throw new RuntimeException("Unable to find unused sAMAccountName value");
			
		}
	}
	
	// Static routine to generate random password
	public static String getRandomPassword() {
		String retval = null;
		retval = RandomStringUtils.randomAlphanumeric(36);  // 36 characters is long enough
		return retval;
		
	}
	
	// Statically remove an AD object
	public static ADUser deleteAdUser(ADUser u) {
		return deleteAdUser(u,null,null);
	}
	public static ADUser deleteAdUser(ADUser u, AuthUser au, String clientip) {
		if (u == null || u.getsAMAccountName() == null || u.getsAMAccountName().equalsIgnoreCase("") || u.getAdDomain() == null || u.getAdDomain().equals("")) {
			return null; // at least we tried
		}
		try {
			DirContext adc = LDAPConnectionFactory.getAdminConnection();
			PCConfig config = PCConfig.getInstance();
			NamingEnumeration<SearchResult> results = null;
			SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			results = adc.search(config.getProperty("ldap.searchbase", true),"samaccountname="+u.getsAMAccountName(),sc);
			while (results != null && results.hasMore()) {
				String dnToRemove = results.next().getNameInNamespace();
				if (dnToRemove == null) {
					return null; // nothing to do here
				}
				//ADConnections adcs = new ADConnections();
				ADConnections adcs = ADConnectionsFactory.getInstance();
				
				for (LDAPAdminConnection lac : adcs.connections) {
					lac.getConnection().destroySubcontext(dnToRemove);
				}
			}
			//writeAuditLog(null,"ADuserDelete",u.getsAMAccountName(),null,null,null);
			if (au != null)
				audit(LOG,"Deleted ad user " + u.getsAMAccountName(),au.getUid(),null,u.getsAMAccountName(),null,clientip);
			else
				audit(LOG,"Deleted ad user " + u.getsAMAccountName(),null,null,u.getsAMAccountName(),null,clientip);
			return u;
		} catch (Exception e) {
			return null;
		}
	}
	
	// Statically create an AD object with POSIX attributes
	public static ADUser createAdUser(ADUser u, AuthUser au) {
		return createAdUser(u,au,null);
	}
	public static ADUser createAdUser(ADUser u, AuthUser au,String clientaddr) {
		// Instead of simply adding on at the end, we actually replace the create operation.  This is more efficient, and 
		// avoids having to carry around POSIX attributes in the AD User object when they're not useful in many (most?) situations.
		//
		if (u == null || u.getsAMAccountName() == null || u.getsAMAccountName().equals("") || u.getAdDomain() == null || u.getAdDomain().equals("")) {
			error(LOG,"createAdUser: empty samaccountname");
			return null;
		}
		
		//ADConnections adc = new ADConnections();
		ADConnections adc = ADConnectionsFactory.getInstance();
		
		SearchControls sc = null;
		PCConfig config = null;
		try {
			config = PCConfig.getInstance();
			sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		} catch (Exception e) {
			error(LOG,"createAdUser: Search scope creation failed");
			return null;
		}
		
		// Attempt POSIX retrieval
		boolean hasPosix = false;
		int uidnumber = 65535;
		int gidnumber = 65535;
		String homedirectory = null;
		String loginshell = null;
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select * from posixuser where uid = ?");
				if (ps != null) {
					ps.setString(1, au.getUid());
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						hasPosix = true;
						uidnumber = rs.getInt("uidnumber");
						gidnumber = rs.getInt("gidnumber");
						homedirectory = rs.getString("homedirectory");
						loginshell = rs.getString("loginshell");
					}
				}
			}
		} catch (Exception e) {
			//ignore
		} finally {
			try {
				if (rs != null) {
					rs.close();
				} 
				if (ps != null) {
					ps.close();
				}
				if (pcdb != null) {
					pcdb.close();
				}
			} catch (Exception e) {
				// ignore
			}
		}
		
		
		try {
			Attributes ba = new BasicAttributes();
			ba.put("samaccountname", u.getsAMAccountName());
			ba.put("userprincipalname", u.getsAMAccountName() + "@" + config.getProperty("ldap.domain", true));
			Attribute oc = new BasicAttribute("objectClass");
			oc.add("top");
			oc.add("person");
			oc.add("organizationalperson");
			oc.add("user");
			ba.put(oc);
			ba.put("givenname", "User");
			ba.put("sn",u.getsAMAccountName());
			ba.put("cn",u.getsAMAccountName());
			ba.put("useraccountcontrol","544"); // disable "must have password" flag
			
			// And POSIX if available
			if (hasPosix) {
				ba.put("uidnumber",String.valueOf(uidnumber));
				ba.put("gidnumber",String.valueOf(gidnumber));
				ba.put("homedirectory",homedirectory);
				ba.put("loginshell",loginshell);
			}
			
			String dn="cn="+u.getsAMAccountName()+","+u.getAdOu();
			
			boolean userCreated = false;
			
			for (LDAPAdminConnection lac : adc.connections) {
				try {
					if (isSamaccountnameAvailable(u.getsAMAccountName(),lac) && ! userCreated) {
						lac.getConnection().createSubcontext(dn,ba);
						debug(LOG,"Initial account creation completed on " + lac.getConnection().getEnvironment().get(DirContext.PROVIDER_URL));
						userCreated = true;  // user should be created everywhere we need eventually -- just propagation to deal with
						Thread.sleep(4000);  // 4-second delay for propagation to catch up
					} else {
							while (isSamaccountnameAvailable(u.getsAMAccountName(),lac)) {
								debug(LOG,"Waiting for propagation again...user=" + u.getsAMAccountName() + " host=" + lac.getConnection().getEnvironment().get(DirContext.PROVIDER_URL));
								// blocking loop to wait for propagation
								Thread.sleep(1000);
							}
							debug(LOG,"Propagation wins!");
					}
				} catch (Exception ign) {
					error(LOG,"Threw " + ign.getMessage() + " while creating AD user - " + u.getsAMAccountName());
					// ignore
				}
			}
			String qPW = "\"" + u.getAdPassword() + "\"";
			byte ub[] = qPW.getBytes("UnicodeLittleUnmarked");
			ModificationItem[] mi = new ModificationItem[1];
			mi[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,new BasicAttribute("UnicodePwd",ub));
			
			
			for (LDAPAdminConnection lac : adc.connections) {
				try {
					lac.getConnection().modifyAttributes(dn, mi);
				} catch (Exception ign2) {
					error(LOG,"Threw " + ign2.getMessage() + " changing password for user");
					//ignore
				}
			}
			// For reasons of ordering in the individual DCs, we do this in two loops
			// Add back the "must have password" flag once password is set
			ModificationItem[] mhp = new ModificationItem[1];
			mhp[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userAccountControl", "512"));
			
			for (LDAPAdminConnection lac : adc.connections) {
				try {
					lac.getConnection().modifyAttributes(dn,  mhp);
				} catch (Exception ign3) {
					error(LOG,"Threw " + ign3.getMessage() + " during require password update");
					//ignore
				}
			}
			// Write out an audit log for what we just did
			audit(LOG,"createADUser",au.getUid(),null,u.getsAMAccountName(),null,clientaddr);
			//writeAuditLog(au.getUid(),"createADUser",u.getsAMAccountName(),null,null,clientaddr);
		} catch (Exception e) {
			error(LOG,"AD User creation threw exception: " + e.getMessage());
			throw new RuntimeException(e);
		}
		u.setCreated(true);
		log(LOG,"AD User creation succeeded");
		return u;
	}
	
	//Statically create an AD object from an ADUser object
	//Because of possible AD propagation delays, we use 
	//a collection of connections and force updates across them
	//all.
	
	public static ADUser createAdUser(ADUser u) {
		if (u == null || u.getsAMAccountName() == null || u.getsAMAccountName().equals("") || u.getAdDomain() == null || u.getAdDomain().equals("")) {
			error(LOG,"createAdUser: empty samaccountname");
			return null;
		}
		
		//ADConnections adc = new ADConnections();
		ADConnections adc = ADConnectionsFactory.getInstance();
		
		SearchControls sc = null;
		PCConfig config = null;
		try {
			config = PCConfig.getInstance();
			sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		} catch (Exception e) {
			error(LOG,"createAdUser: Search scope creation failed");
			return null;
		}
		
		try {
			Attributes ba = new BasicAttributes();
			ba.put("samaccountname", u.getsAMAccountName());
			ba.put("userprincipalname", u.getsAMAccountName() + "@" + config.getProperty("ldap.domain", true));
			Attribute oc = new BasicAttribute("objectClass");
			oc.add("top");
			oc.add("person");
			oc.add("organizationalperson");
			oc.add("user");
			ba.put(oc);
			ba.put("givenname", "User");
			ba.put("sn",u.getsAMAccountName());
			ba.put("cn",u.getsAMAccountName());
			ba.put("useraccountcontrol","544"); // disable "must have password" flag
			
			String dn="cn="+u.getsAMAccountName()+","+u.getAdOu();
			
			boolean userCreated = false;
			
			for (LDAPAdminConnection lac : adc.connections) {
				try {
					if (isSamaccountnameAvailable(u.getsAMAccountName(),lac) && ! userCreated) {
						lac.getConnection().createSubcontext(dn,ba);
						debug(LOG,"Initial account creation completed on " + lac.getConnection().getEnvironment().get(DirContext.PROVIDER_URL));
						userCreated = true;  // user should be created everywhere we need eventually -- just propagation to deal with
						Thread.sleep(4000);  // 4-second delay for propagation to catch up
					} else {
							while (isSamaccountnameAvailable(u.getsAMAccountName(),lac)) {
								debug(LOG,"Waiting for propagation again...user=" + u.getsAMAccountName() + " host=" + lac.getConnection().getEnvironment().get(DirContext.PROVIDER_URL));
								// blocking loop to wait for propagation
								Thread.sleep(1000);
							}
							debug(LOG,"Propagation wins!");
					}
				} catch (Exception ign) {
					error(LOG,"Threw " + ign.getMessage() + " while creating AD user - " + u.getsAMAccountName());
					// ignore
				}
			}
			String qPW = "\"" + u.getAdPassword() + "\"";
			byte ub[] = qPW.getBytes("UnicodeLittleUnmarked");
			ModificationItem[] mi = new ModificationItem[1];
			mi[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,new BasicAttribute("UnicodePwd",ub));
			
			
			for (LDAPAdminConnection lac : adc.connections) {
				try {
					lac.getConnection().modifyAttributes(dn, mi);
				} catch (Exception ign2) {
					error(LOG,"Threw " + ign2.getMessage() + " changing password for user");
					//ignore
				}
			}
			// For reasons of ordering in the individual DCs, we do this in two loops
			// Add back the "must have password" flag once password is set
			ModificationItem[] mhp = new ModificationItem[1];
			mhp[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userAccountControl", "512"));
			
			for (LDAPAdminConnection lac : adc.connections) {
				try {
					lac.getConnection().modifyAttributes(dn,  mhp);
				} catch (Exception ign3) {
					error(LOG,"Threw " + ign3.getMessage() + " during require password update");
					//ignore
				}
			}
			// Write out an audit log for what we just did
			audit(LOG,"createADUser",u.getsAMAccountName(),null,null,null,null);
			// writeAuditLog(null,"createADUser",u.getsAMAccountName(),null,null,null);
		} catch (Exception e) {
			error(LOG,"AD User creation threw exception: " + e.getMessage());
			throw new RuntimeException(e);
		}
		u.setCreated(true);
		log(LOG,"AD User creation succeeded");
		return u;
	}
	
	// Set a new random password for an existing AD user
	public static ADUser setRandomADPassword(ADUser adu) {
		String password = null;
		PCConfig config = PCConfig.getInstance();
		try {
			password = ProconsulUtils.getRandomPassword();
		} catch (Exception e) {
			error(LOG,"Unable to set random password -- returning original ADUser");
			return adu;  // leave it alone if we can't set a password
		}
		// We have a password -- set it in the object and in the AD.
		//
		// ADConnections adcs = new ADConnections();
		ADConnections adcs = ADConnectionsFactory.getInstance();
		DirContext adc = null;
		NamingEnumeration<SearchResult> results = null;
		
		
		SearchControls sc = null;
		try {
			sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			adc = LDAPConnectionFactory.getAdminConnection();
			String AdDn = null;
			if (adc != null) {
				// Search for the user
				results = adc.search(config.getProperty("ldap.searchbase", true),"(sAMAccountName="+adu.getsAMAccountName()+")",sc);
				if (results == null || ! results.hasMore()) {
					error(LOG,"Unable to find AD user during password randomization");
					return adu;
				}
				AdDn = results.next().getNameInNamespace();
				// And update the password
				String qPW = "\"" + password + "\"";
				byte ub[] = qPW.getBytes("UnicodeLittleUnmarked");
				ModificationItem[] mi = new ModificationItem[1];
				mi[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,new BasicAttribute("UnicodePwd",ub));
				
				
				for (LDAPAdminConnection lac : adcs.connections) {
					try {
						lac.getConnection().modifyAttributes(AdDn, mi);
					} catch (Exception ign2) {
						error(LOG,"Threw " + ign2.getMessage() + " during randomization of password");
						//ignore
					}
				}
				adu.setAdPassword(password);
				return adu;
			} else {
				return adu;
			}
			
		} catch (Exception e) {
			error(LOG,"Failed setting random password in AD -- returning original ADUser");
			return adu;
		}
		
	}
	
	// Given an authuser and a target machine, get the associated static AD user (if it exists)
	//
	public static ADUser getStaticADUser(AuthUser au, String fqdn) {
		String userid = null;
		String password = null;
		
		ADUser retval = new ADUser();
		
		PCConfig config = PCConfig.getInstance();
				
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select targetuser from static_host where eppn = ? and fqdn = ?");
				if (ps != null) {
					ps.setString(1, au.getUid());
					ps.setString(2,  fqdn);
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						// If there's more than one, last one in wins
						userid = rs.getString("targetuser");
					}
				}
			}
		} catch (Exception e) {
			//ignore
		} finally {
			try {
				if (rs != null) {
					rs.close();
				} 
				if (ps != null) {
					ps.close();
				}
				if (pcdb != null) {
					pcdb.close();
				}
			} catch (Exception e) {
				// ignore
			}
		}
		
		retval.setsAMAccountName(userid);
		retval.setAdDomain(config.getProperty("ldap.domain",true));
		if (config.getProperty("ldap.staticbase", false) != null) {
			retval.setAdOu(config.getProperty("ldap.staticbase", true));  // use config option if set
		} else {
			retval.setAdOu("ou=static,"+config.getProperty("ldap.targetbase",true));  // else static users are in a "static" sub-ou of targetbase by fiat
		}
		retval.setCreated(true); // already there
		
		retval = ProconsulUtils.setRandomADPassword(retval);  // set random password and add it to the ADUser object
		
		return retval;
	}
	
	// Given an authuser, create a randomized AD user with the right POSIX attributes
	public static ADUser createRandomizedADUser(AuthUser au) {
		String userid = null;
		String password = null;
		try {
			userid = ProconsulUtils.getRandomSamaccountname();
			password=ProconsulUtils.getRandomPassword();
		} catch (Exception e) {
			error(LOG,"Exception in createRandomizedADUser: " + e.getMessage());
			return null;
		}
		PCConfig config = PCConfig.getInstance();
		ADUser create = new ADUser();
		create.setsAMAccountName(userid);
		create.setAdPassword(password);;
		create.setAdDomain(config.getProperty("ldap.domain", true));
		create.setAdOu(config.getProperty("ldap.targetbase", true));
		create.setCreated(false);
		
		ADUser retval = ProconsulUtils.createAdUser(create,au);  // create for specific authuser
		return retval;
	}
	
	// Create an ADUser using random sAMAccountName and password
	public static ADUser createRandomizedADUser() {
		String userid = null;
		String password = null;
		try {
			userid = ProconsulUtils.getRandomSamaccountname();
			password = ProconsulUtils.getRandomPassword();
		} catch (Exception e) {
			error(LOG,"Exception in createRandomizedADUser:  " + e.getMessage());
			return null;
		}
		PCConfig config = PCConfig.getInstance();
		ADUser create = new ADUser();
		create.setsAMAccountName(userid);;
		create.setAdPassword(password);
		create.setAdDomain(config.getProperty("ldap.domain", true));
		create.setAdOu(config.getProperty("ldap.targetbase", true));
		create.setCreated(false);;
		
		ADUser retval = ProconsulUtils.createAdUser(create);
		
		return retval;
	}
	
	// Get a randomized password suitable for VNC use
	public static String getVncPassword() {
		return RandomStringUtils.randomAlphanumeric(11);
	}
	
	// CSRF validation
	
	public static boolean checkOrigin(HttpServletRequest req) {
		String origin = req.getHeader("Origin");
		if (origin == null) {
			origin = req.getHeader("Referrer");
			if (origin == null) {
				return true;  // no origin to validate against
			}
		}
		origin = origin.replaceAll("^.*//","");
		origin = origin.replaceAll("[:/].*$", "");
		if (req.getServerName().equalsIgnoreCase(origin)) {
			return true;
		} else {
			return false;
		}
	}
	public static String getSessionCSRFToken(HttpServletRequest req) {
		// Here, we either (a) create a session and a token, store and return it, 
		// or (b) retrieve and return the existin session's token.
		//
		// First check for existing session token
		Connection pcdb = null;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		HttpSession httpSession = req.getSession();
		String token = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select * from csrf_tokens where sessionId = ?");
				ps.setString(1, httpSession.getId());
				rs = ps.executeQuery();
				if (rs != null && rs.next()) {
					token = rs.getString("csrfToken");
					long last = rs.getInt("lastused");
					ps2 = pcdb.prepareStatement("update csrf_tokens set lastused = ? where sessionId = ?");
					ps2.setLong(1,System.currentTimeMillis()/1000);
					ps2.setString(2,  httpSession.getId());
					ps2.executeUpdate();
					return token;
				} else {
					// Create an entry and return the value
					token = UUID.randomUUID().toString();
					ps2 = pcdb.prepareStatement("insert into csrf_tokens values (?,?,?)");
					ps2.setString(1, httpSession.getId());
					ps2.setString(2, token);
					ps2.setLong(3,System.currentTimeMillis()/1000);
					ps2.executeUpdate();
					return token;
				}
			} else {
				// No database connection -- return null
				return token;
			}
		} catch (Exception e) {
			return token;
		} finally {
			try {
				if (rs2 != null) {
					rs2.close();
				}
				if (rs != null) {
					rs.close();
				}
				if (ps2 != null) {
					ps2.close();
				}
				if (ps != null) {
					ps.close();
				}
				if (pcdb != null) {
					pcdb.close();
				}
			} catch (Exception ign) {
				// ignore
			}
		}
	}
		
	
	// TODO:  Authorization is getting much more complicated now
	// Need to authorize based on type of access as well as user and fqdn
	// and look for authorization in more than just the URN pattern for group 
	// name
	//
	// This needs to be refactored and possibly turned into multiple
	// isAuthorizedByXFACTOR() routines for different XFACTOR values
	//
	// Given an authuser, an fqdn, and a list of groups, determine if
	// the authuser is authorized to them.
	// Initially, only validates hostname (not group name[s])
	//
	
	public static boolean isAuthorized(AuthUser au, String fqdn, ArrayList<String> mem) {
		if (au == null || fqdn == null || fqdn.equals("")) {
			// nobody gets nothing
			return false;
		}
		if (! ProconsulUtils.canUseApp(au)) {
			// if you're not able to use the app, you get nothing
			return false;
		}
		ArrayList<String> amem = au.getMemberships();
		
		for (String oldval : amem) {
			String newval = oldval.toUpperCase();
			int idx = amem.indexOf(oldval);
			amem.set(idx,newval);
		}
		
		for (String test : mem) {
			test = "urn:mace:duke.edu:groups:orgs:" + test + ":ad_admins";
			if (! amem.contains(test.toUpperCase())) {
				return false;
			}
		}
		return true;
	}
	
	// VNC Port Number acquisition.  We arrange to encumber the 
	// port number in the database before we return it.  
	// Session data is used to encumber the number
			
	public static int getVncPortNumber(ProconsulSession sess) {
		// Find an (even-numbered) unused port by +2-ing the highest
		// numbered port in the database and rely on attrition to
		// eventually free up intervening ports.
		//
		// Max out at 5998, in which case we start looking back at 0 again and search until we find an option or 
		// exceed the limit again.
		// Addresses port exhaustion problem.
		
		int retval = 0;
		if (sess == null) {
			throw new RuntimeException("null session getting vnc port number");
		}
		Connection pcdb = null;
		try {
		pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb == null) {
				throw new RuntimeException("Database Connection failed getting vnc port number");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		ResultSet rs = null;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		
		try {
			ps = pcdb.prepareStatement("select max(portnum) as portnum from proconsul.ports where portnum is not null");
			if (ps == null) {
				throw new RuntimeException("Null preparedstatement getting vnc port number");
			}
			rs = ps.executeQuery();
			while (rs != null && rs.next()) {
				retval = rs.getInt("portnum");
				if (retval == 0) {
					retval = 2;
				} else {
					retval = retval + 2;
				}
				// Check for wrap around
				if (retval >= 100) {
					// wrapped -- perform the more expensive search
					ResultSet rs3 = null;
					PreparedStatement ps3 = null;
					for (int i = 2; i < 100; i+=2) {
						ps3 = pcdb.prepareStatement("select portnum from proconsul.ports where portnum = ?");
						ps3.setInt(1,i);
						rs3 = ps3.executeQuery();
						if (rs3 == null || ! rs3.next()) {
							retval = i;
							rs3.close();
							ps3.close();
							break;
						}
					}
				}
				// And die now if we're actually exhausted
				if (retval >= 100) {
					throw new RuntimeException("Port exhaustion");
				}
				// Otherwise, use the port we found
				ps2 = pcdb.prepareStatement("insert into proconsul.ports values(?,?,?,?)");
				ps2.setInt(1, retval);
				if (sess.getFqdn() != null) {
					ps2.setString(2, sess.getFqdn());
				} else {
					ps2.setNull(2,java.sql.Types.VARCHAR);
				}
				if (sess.getTargetUser() != null) {
					if (sess.getTargetUser().getsAMAccountName() != null) {
						ps2.setString(3,sess.getTargetUser().getsAMAccountName());
					} else {
						ps2.setNull(3,java.sql.Types.VARCHAR);
					}
				} else {
					ps2.setNull(3,java.sql.Types.VARCHAR);
				}
				if (sess.getOwner() != null) {
					if (sess.getOwner().getUid() != null) {
						ps2.setString(4,sess.getOwner().getUid());
					} else {
						ps2.setNull(4, java.sql.Types.VARCHAR);
					}
				} else {
					ps2.setNull(4,java.sql.Types.VARCHAR);
				}
				ps2.executeUpdate();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (ps2 != null) {
				try {
					ps2.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return retval;
	}
	
	// Write session to the database
	public static void writeSessionToDB(ProconsulSession sess) {
		writeSessionToDB(sess,null);
	}
	public static void writeSessionToDB(ProconsulSession sess, String clientaddr) {
		Connection pcdb = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		
		String log_eppn = null;
		String log_targetuser = null;
		String log_targethost = null;
		
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb == null) {
				throw new RuntimeException("Null pcdb writing out session");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		try {
			ps = pcdb.prepareStatement("select * from sessions where upper(owner) = upper(?) and upper(fqdn) = upper(?) and upper(samaccountname) = upper(?)");
			if (ps == null) {
				throw new RuntimeException("Failed preparing query to write session");
			}
			if (sess.getOwner() == null || sess.getOwner().getUid() == null) {
				ps.setNull(1,java.sql.Types.VARCHAR);
			} else {
				ps.setString(1, sess.getOwner().getUid());
				log_eppn = sess.getOwner().getUid();
			}
			ps.setString(2,sess.getFqdn());
			if (sess.getTargetUser() == null || sess.getTargetUser().getsAMAccountName() == null) {
				ps.setNull(3, java.sql.Types.VARCHAR);
			} else {
				ps.setString(3, sess.getTargetUser().getsAMAccountName());
				log_targetuser = sess.getTargetUser().getsAMAccountName();
			}
			rs = ps.executeQuery();
			
			// Excessive debug logging

			if (sess.getOwner() == null) {
				debug(LOG,"Session.owner is null!");
			}
			
			if (sess.getTargetUser() == null) {
				debug(LOG,"Session.targetuser is null!");
			}
			
			debug(LOG,"Session seems to be populated");
			
			if (sess.getOwner().getUid() == null) {
				debug(LOG,"Session.owner has no uid");
			} else {
				debug(LOG,"Session.owner.uid is " + sess.getOwner().getUid());
			}
			
			if (sess.getTargetUser().getsAMAccountName() == null) {
				debug(LOG,"Session.targetuser has no samaccountname");
			} else {
				debug(LOG,"Session.targetuser.samaccountname is " + sess.getTargetUser().getsAMAccountName());
			}
			
			if (rs == null || ! rs.next()) {
				log(LOG,"Could not find session for " + sess.getOwner().getUid() + "," + sess.getFqdn() + "," + sess.getTargetUser().getsAMAccountName());
				ps2 = pcdb.prepareStatement("insert into sessions values(?,?,?,?,?,?,?,?,?,NULL,?,?,?,?)");
			
				ps2.setString(1,sess.getFqdn());
				log_targethost = sess.getFqdn();
				if (sess.getOwner() == null || sess.getOwner().getUid() == null) {
					ps2.setNull(2,java.sql.Types.VARCHAR);
					log_eppn = "NoUser";
				} else {
					ps2.setString(2, sess.getOwner().getUid());
					log_eppn = sess.getOwner().getUid();
				}
				ps2.setString(3, sess.getDisplayName());
				ps2.setInt(4, sess.getNovncPort());
				ps2.setString(5, sess.getVncPassword());
				if (sess.isConnected()) {
					ps2.setInt(6,1);
				} else {
					ps2.setInt(6, 0);
				}
				if (sess.getTargetUser()==null || sess.getTargetUser().getsAMAccountName() == null) {
					ps2.setNull(7, java.sql.Types.VARCHAR);
				} else {
					ps2.setString(7, sess.getTargetUser().getsAMAccountName());
					log_targetuser = sess.getTargetUser().getsAMAccountName();
				}
				if (sess.isAvailable()) {
					ps2.setInt(8,1);
				} else {
					ps2.setInt(8, 0);
				}
				ps2.setString(9, sess.printableStartTime());
				if (sess.getType() != null) {
					ps2.setString(10, sess.getType());
				} else {
					ps2.setNull(10,java.sql.Types.VARCHAR);
				}
				if (sess.getDelegatedou() != null) {
					ps2.setString(11,  sess.getDelegatedou());
				} else {
					ps2.setNull(11, java.sql.Types.VARCHAR);
				}
				if (sess.getDelegatedrole() != null) {
					ps2.setString(12,  sess.getDelegatedrole());
				} else {
					ps2.setNull(12, java.sql.Types.VARCHAR);
				}
				if (sess.getGatewayfqdn() != null) {
					ps2.setString(13,  sess.getGatewayfqdn());
				} else {
					ps2.setNull(13,  java.sql.Types.VARCHAR);
				}
				ps2.executeUpdate();
				audit(LOG,"startSession",log_eppn,log_targethost,log_targetuser,null,clientaddr);
				//writeAuditLog(log_eppn,"startSession",log_targetuser,log_targethost,null,clientaddr);
				return;
			} else {
				// on update, no need to reset type or ou/role -- it was set on create...
				ps2 = pcdb.prepareStatement("update sessions set displayname = ?, novncport = ?, vncpassword = ?, connected = ?, running = ?, status = ? where owner = ? and fqdn = ? and samaccountname= ?");
				ps2.setString(1, sess.getDisplayName());
				ps2.setInt(2, sess.getNovncPort());
				ps2.setString(3, sess.getVncPassword());
				if (sess.isConnected()) {
					ps2.setInt(4, 1);
				} else {
					ps2.setInt(4,0);
				}
				if (sess.isAvailable()) {
					ps2.setInt(5,1);
				} else {
					ps2.setInt(5, 0);
				}
				switch(sess.getStatus()) {
				case STARTING:
					ps2.setString(6, "starting");
					break;
				case CONNECTED:
					ps2.setString(6,"connected");
					break;
				case DISCONNECTED:
					ps2.setString(6,"disconnected");
					break;
				case TERMINATING:
					ps2.setString(6,"terminating");
					break;
				default:
					ps2.setString(6, "unknown");
					break;
				}
				if (sess.getOwner() == null || sess.getOwner().getUid() == null) {
					ps2.setNull(7, java.sql.Types.VARCHAR);
					log_eppn = "NoUser";
				} else {
					ps2.setString(7, sess.getOwner().getUid());
					log_eppn = sess.getOwner().getUid();
				}
				ps2.setString(8, sess.getFqdn());
				if (sess.getTargetUser() == null || sess.getTargetUser().getsAMAccountName() == null) {
					ps2.setNull(9,java.sql.Types.VARCHAR);
				} else {
					ps2.setString(9, sess.getTargetUser().getsAMAccountName());
					log_targetuser = sess.getTargetUser().getsAMAccountName();
				}
				ps2.executeUpdate();
				switch(sess.getStatus()) {
				case STARTING:
					audit(LOG,"starting",log_eppn,sess.getFqdn(),log_targetuser,null,clientaddr);
					//writeAuditLog(log_eppn,"starting",log_targetuser,sess.getFqdn(),null,clientaddr);
					break;
				case CONNECTED:
					audit(LOG,"connected",log_eppn,sess.getFqdn(),log_targetuser,null,clientaddr);
					//writeAuditLog(log_eppn,"connected",log_targetuser,sess.getFqdn(),null,clientaddr);
					break;
				case DISCONNECTED:
					audit(LOG,"disconnected",log_eppn,sess.getFqdn(),log_targetuser,null,clientaddr);
					//writeAuditLog(log_eppn,"disconnected",log_targetuser,sess.getFqdn(),null,clientaddr);
					break;
				case TERMINATING:
					audit(LOG,"terminating",log_eppn,sess.getFqdn(),log_targetuser,null,clientaddr);
					//writeAuditLog(log_eppn,"terminating",log_targetuser,sess.getFqdn(),null,clientaddr);
					break;
				}
				return;
			}
		} catch (Exception e) {
			error(LOG,"Throwing exception: " + e.getMessage() + " writing session to DB");
			throw new RuntimeException(e);
		} finally {
			if (ps2 != null) {
				try {
					ps2.close();
				} catch (Exception e) {
					//ignore
				}
			}
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception e) {
					//ignore
				}
			}
		}
	}
	
	public static void deletePortFromDB(int portnum) {
		Connection pcdb = null;
		PreparedStatement ps = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb == null) {
				error(LOG,"Failed database connection deleting port number " + portnum);
				throw new RuntimeException("Failed database connection deleting port number " + portnum);
			}
		} catch (Exception e) {
			error(LOG,"Caught exception " + e.getMessage() + " during databaase connection for deletePortFromDB");
			throw new RuntimeException(e);
		}
		try {
			ps = pcdb.prepareStatement("delete from ports where portnum = ?");
			ps.setInt(1, portnum);;
			ps.executeUpdate();
			return;
		} catch (Exception e) {
			// ignore
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}
	
	// When a session goes away completely, we need to remove it too
	
	public static void deleteSessionFromDB(ProconsulSession sess) {
		Connection pcdb = null;
		PreparedStatement ps = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb == null) {
				error(LOG,"Failed database connection deleting session");
				throw new RuntimeException("Failed database connection deleting session");
			}
		} catch (Exception e) {
			error(LOG,"Exception " + e.getMessage() + " during database connection for session deletion");
			throw new RuntimeException(e);
		}
		try {
			ps = pcdb.prepareStatement("delete from sessions where fqdn = ? and owner = ? and novncport = ?");
			ps.setString(1,sess.getFqdn());
			if (sess.getOwner() == null || sess.getOwner().getUid() == null) {
				ps.setNull(2,java.sql.Types.VARCHAR);
			} else {
				ps.setString(2, sess.getOwner().getUid());
			}
			ps.setInt(3, sess.getNovncPort());
			
			ps.executeUpdate();
		} catch (Exception e) {
			//ignore
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		ProconsulUtils.deletePortFromDB(sess.getNovncPort());
	}
	
	// Explicitly add a workstation restriction to a user object
	public static ADUser addWorkstationToAdUser(String fqdn, ADUser u) {
		return addWorkstationToADUser(fqdn,u,null,null);
	}
	
	public static ADUser addWorkstationToADUser(String fqdn, ADUser u, AuthUser au, String clientip) {
		PCConfig config = PCConfig.getInstance();
		
		String orgBase = config.getProperty("ldap.orgbase", true);
		
		fqdn = fqdn.replaceAll(" ", "");  // strip spaces from fqdn input
		
		if (! fqdn.matches("^[A-Za-z0-9.,_-]+")) {
			error(LOG,"illegal host restriction string ignored in session for target user " + u.getsAMAccountName());
			return u;
		}
		
		// ADConnections adc = new ADConnections();
		ADConnections adc = ADConnectionsFactory.getInstance();
		NamingEnumeration<SearchResult> results = null;
		NamingEnumeration<SearchResult> sr = null;
		SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		DirContext dc = null;
		DirContext dc2 = null;
		DirContext dc3 = null;
		try {
			dc = LDAPConnectionFactory.getAdminConnection();
			if (dc == null) {
				return u;
			}
		} catch (Exception e) {
			return u;
		}
		
		try {
			results = dc.search(config.getProperty("ldap.searchbase", true), "samaccountname="+u.getsAMAccountName(),sc);
			if (results == null || ! results.hasMore()) {
				return u;
			}
			String userDn = results.next().getNameInNamespace();

			// Acquire the SMB name from the AD for the fqdn and gateway fqdn we have
			String uws = "";
			try {
				dc2 = LDAPConnectionFactory.getAdminConnection();
				SearchControls scon = new SearchControls();
				scon.setSearchScope(SearchControls.SUBTREE_SCOPE);
				String basedn = config.getProperty("ldap.searchbase", true);
				sr = dc2.search(basedn, "(&(objectcategory=computer)(dnshostname="+fqdn+"))",scon);
				while (sr != null && sr.hasMore()) {
					String n = (String) sr.next().getAttributes().get("cn").get();
					uws = n;
				}
			} catch (Exception e) {
				// ignore
			}

			// Add the userWorkstations value to the user if one exists
			if (uws.equals("")) {
				error(LOG,"No workstation name found for " + fqdn);
				return u;
			}
			
			// Microsoft in their infinite wisdom makes this a single-valued attribute with a 
			// comma-separated list of values in it.  Go figure.
			
			String pre = "";
			try {
				dc3 = LDAPConnectionFactory.getAdminConnection();
				SearchControls scon = new SearchControls();
				scon.setSearchScope(SearchControls.OBJECT_SCOPE);
				sr = dc3.search(userDn,"(userWorkstations=*)",scon);
				while (sr != null && sr.hasMore()) {
					pre = (String) sr.next().getAttributes().get("userWorkstations").get();
				}
			} catch (Exception e) {
				// ignore
			}
			if (pre.equals("")) {
				pre = uws;
			} else {
				pre = pre + "," + uws;
			}
			
			ModificationItem[] mods = new ModificationItem[1];
			if (pre.contains(",")) {
				mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userWorkstations",pre));
			} else {
				mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("userWorkstations",pre));
			}

			
			for (LDAPAdminConnection lac : adc.connections) {
				try {
					lac.getConnection().modifyAttributes(userDn, mods);
					if (au != null)
						audit(LOG,"limitWS",au.getUid(),uws,u.getsAMAccountName(),null,clientip);
					else
						audit(LOG,"limitWS",null,uws,u.getsAMAccountName(),null,clientip);
					//writeAuditLog(null,"limitWS",u.getsAMAccountName(),null,uws,null);
				} catch (Exception ign) {
					error(LOG,"Threw exception " + ign.getMessage() + " on addWsRestriction for " + u.getsAMAccountName() + "," + uws);
					// ignore
				}
			}
		} catch (Exception e) {
			return u;
		} finally {
			if (results != null) {
				try {
					results.close();
				} catch (Exception e) {
					//ignore
				}
			}
			if (sr != null) {
				try {
					sr.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (adc != null) {
				try {
					adc.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (dc != null) {
				try {
					dc.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (dc2 != null) {
				try {
					dc2.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return u;
	}
	
	// Add appropriate userWorkstations constraint to the user based on session information
	public static ADUser addWorkstationsToADUser(ProconsulSession sess, ADUser u) {
		return addWorkstationsToADUser(sess,u,null);
	}
	public static ADUser addWorkstationsToADUser(ProconsulSession sess, ADUser u, String clientip) {
		PCConfig config = PCConfig.getInstance();
		
		String orgBase = config.getProperty("ldap.orgbase", true);
		
		if (sess == null || u == null) {
			return u;
		}
		
		// ADConnections adc = new ADConnections();
		ADConnections adc = ADConnectionsFactory.getInstance();
		NamingEnumeration<SearchResult> results = null;
		NamingEnumeration<SearchResult> sr = null;
		SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		DirContext dc = null;
		DirContext dc2 = null;
		try {
			dc = LDAPConnectionFactory.getAdminConnection();
			if (dc == null) {
				return u;
			}
		} catch (Exception e) {
			return u;
		}
		
		try {
			results = dc.search(config.getProperty("ldap.searchbase", true), "samaccountname="+u.getsAMAccountName(),sc);
			if (results == null || ! results.hasMore()) {
				return u;
			}
			String userDn = results.next().getNameInNamespace();

			// Acquire the SMB name from the AD for the fqdn and gateway fqdn we have
			String uws = "";
			try {
				dc2 = LDAPConnectionFactory.getAdminConnection();
				SearchControls scon = new SearchControls();
				scon.setSearchScope(SearchControls.SUBTREE_SCOPE);
				String basedn = config.getProperty("ldap.searchbase", true);
				if (sess.getFqdn() != null && ! sess.getFqdn().equals("")) {
					sr = dc2.search(basedn, "(&(objectcategory=computer)(dnshostname="+sess.getFqdn()+"))",scon);
					while (sr != null && sr.hasMore()) {
						// had better be only one box with this DNS name, right?
						String n = (String) sr.next().getAttributes().get("sAMAccountName").get();
						uws = n;
					}
				}
				if (sess.getGatewayfqdn() != null && ! sess.getGatewayfqdn().equals("")) {
					sr = dc.search(basedn, "(&(objectcategory=computer, cons)(dnshostname="+sess.getGatewayfqdn()+"))",scon);
					while (sr != null && sr.hasMore()) {
						String n = (String) sr.next().getAttributes().get("sAMAccountName").get();
						uws += ","+n;
					}
				}
				String myhost=config.getProperty("proconsul.servername", false);
				if (myhost != null) {
					uws += ","+myhost;
				}
			} catch (Exception e) {
				// ignore
			}

			// Add the userWorkstations value to the user if one exists
			if (uws.equals("")) {
				error(LOG,"No worsktation name found for " + sess.getFqdn() + " and/or " + sess.getGatewayfqdn());
				return u;
			}
			
			ModificationItem[] mods = new ModificationItem[1];
			mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("userWorkstations",uws));

			
			for (LDAPAdminConnection lac : adc.connections) {
				try {
					lac.getConnection().modifyAttributes(userDn, mods);
					if (sess.getOwner() != null)
						audit(LOG,"limitWS",sess.getOwner().getUid(),uws,u.getsAMAccountName(),null,clientip);
					else
						audit(LOG,"limitWS",null,uws,u.getsAMAccountName(),null,clientip);
					//writeAuditLog(null,"limitWS",u.getsAMAccountName(),null,uws,null);
				} catch (Exception ign) {
					error(LOG,"Threw exception " + ign.getMessage() + " on addWsRestriction for " + u.getsAMAccountName() + "," + uws);
					// ignore
				}
			}
		} catch (Exception e) {
			return u;
		} finally {
			if (results != null) {
				try {
					results.close();
				} catch (Exception e) {
					//ignore
				}
			}
			if (sr != null) {
				try {
					sr.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (adc != null) {
				try {
					adc.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (dc != null) {
				try {
					dc.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (dc2 != null) {
				try {
					dc2.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return u;
	}
	
	// Add a group specified by a URN to a user identified by an ADUser object
	public static ADUser addGroupToADUser(String urn, ADUser u) {
		return addGroupToADUser(urn,u,null,null);
	}
	public static ADUser addGroupToADUser(String urn, ADUser u, AuthUser au, String clientip) {
		PCConfig config = PCConfig.getInstance();
		
		String orgBase = config.getProperty("ldap.orgbase", true);
		
		if (urn == null || u == null) {
			return u;
		}
		
		// ADConnections adc = new ADConnections();
		ADConnections adc = ADConnectionsFactory.getInstance();
		NamingEnumeration<SearchResult> results = null;
		SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		DirContext dc = null;
		try {
			dc = LDAPConnectionFactory.getAdminConnection();
			if (dc == null) {
				return u;
			}
		} catch (Exception e) {
			return u;
		}
		
		try {
			results = dc.search(config.getProperty("ldap.searchbase", true), "samaccountname="+u.getsAMAccountName(),sc);
			if (results == null || ! results.hasMore()) {
				return u;
			}
			String userDn = results.next().getNameInNamespace();
			
			String groupDN = orgBase;
			String[] parts = urn.split(":");
			
			if (! urn.contains(":")) {
				groupDN = urn;
			} else {
				for (int i = 0; i < parts.length; i ++) {
					groupDN = "ou="+parts[i]+","+groupDN;
				}
				groupDN = "cn=ad_admins,"+groupDN;
			}
			
			// And the user to the group
			
			ModificationItem[] mods = new ModificationItem[1];
			mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE,new BasicAttribute("member",userDn));
			
			for (LDAPAdminConnection lac : adc.connections) {
				try {
					lac.getConnection().modifyAttributes(groupDN, mods);
					if (au != null) 
						audit(LOG,"addGroup",au.getUid(),null,u.getsAMAccountName(),urn,clientip);
					else
						audit(LOG,"addGroup",null,null,u.getsAMAccountName(),urn,clientip);
					//writeAuditLog(null,"addGroup",u.getsAMAccountName(),null,urn,null);
				} catch (Exception ign) {
					error(LOG,"Threw exception " + ign.getMessage() + " on addGroup for " + u.getsAMAccountName() + "," + urn);
					// ignore
				}
			}
		} catch (Exception e) {
			return u;
		} finally {
			if (results != null) {
				try {
					results.close();
				} catch (Exception e) {
					//ignore
				}
			}
			if (adc != null) {
				try {
					adc.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (dc != null) {
				try {
					dc.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		
		if (u.getMemberships() == null) {
			return u;
		}
		if (u.getMemberships().contains(urn)) {
			return u;
		} else {
			u.getMemberships().add(urn);
			return u;
		}
	}
	
	
	
	// Routines for determining capabilities from the DB
	
	public static boolean canAdminFQDN(AuthUser au, String fqdn) {
		// Authorization routine to determine whether the authenticated user has rights 
		// sufficient to be a delegated admin over host X.
		// We enumerate the groups the user is a member of, then check each group 
		// to determine what if any OU it affords access to.  If it affords access to
		// an OU, we enumerate the machines in the OU and check to see if the given 
		// machine is in the list.  If we find the machine in a managed OU, we return
		// true.  Otherwise, we return false.
		//
		// Authorization routine for delegated admin connection to an FQDN
		//
		boolean retval = false;
		PreparedStatement ps = null;
		Connection pcdb = null;
		ResultSet rs = null;
		// Check that the authuser is a member of some groups
		if (au.getMemberships() == null || au.getMemberships().isEmpty()) {
			debug(LOG,"User " + au.getUid() + " has no membership and thus is not a delegate");
			return false;
		}
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				// See if the user has any delegating groups -- if so, check them
				ps = pcdb.prepareStatement("select groupurn,ou from delegate_groups where groupurn = ?");
				if (ps != null) {
					Iterator<String> iter = au.getMemberships().iterator();
					while (retval == false && iter != null && iter.hasNext()) {
						String checkMembership = iter.next();
						ps.setString(1, checkMembership);
						rs = ps.executeQuery();
						if (rs != null && rs.next()) {
							// This one is a delegate group -- check the ou that it pertains to
							String delegateOu = rs.getString("ou");
							ArrayList<String> delegatedFQDNs = fqdnsForOu(delegateOu);
							if (delegatedFQDNs != null && ! delegatedFQDNs.isEmpty()) {
								// Check and see if our fqdn is contained
								if (delegatedFQDNs.contains(fqdn)) {
									// User is member of a group that controls an OU that the fqdn is in
									// so return true
									return true;
								}
							}
						}
					}
					return false;  // if we don't find one, return false
				} else {
					return false; // fail closed
				}
			} else {
				return false;  // fail closed
			}
		} catch (Exception e) {
			error(LOG,"Failed to validate delegated admin rights for " + fqdn + "by user " + au.getUid() + " due to exception " + e.getMessage());
			return false;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	public static boolean canUseCO(AuthUser au) {
		boolean retval = false;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		Connection pcdb = null;
		Connection pcdb2 = null;
		Connection pcdb3 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		
		if (au.getUid() == null || au.getUid().equalsIgnoreCase("")) {
			error(LOG,"Userid not available - no CO access allowed");
			return false;
		} else {
			try {
				pcdb2 = DatabaseConnectionFactory.getProconsulDatabaseConnection();
				if (pcdb2 != null) {
					ps2 = pcdb2.prepareStatement("select eppn from access_user where eppn = ? and type = 'co'");
					if (ps2 != null) {
						ps2.setString(1,  au.getUid());
						rs2 = ps2.executeQuery();
						if (rs2 != null && rs2.next()) {
							log(LOG,"User " + au.getUid() + " is directly authorized to use CheckOut feature");
							return true;
						}
					}
				}
			} catch (Exception e) {
				return false; // if we except on checking, fail
			} finally {
				if (rs2 != null) {
					try {
						rs2.close();
					} catch (Exception ign) {
						// ignore
					}
				}
				if (ps2 != null) {
					try {
						ps2.close();
					} catch (Exception ign) {
						// ignore
					}
				}
				if (pcdb2 != null) {
					try {
						pcdb2.close();
					} catch (Exception ign) {
						// ignore
					}
				}
			}
			
			// For now, only explicit authorizations are available 
			// Later add groups and entitlements to this space
			//
			
			return false;
		}
		
	}
	public static boolean canUseDA(AuthUser au) {
		// Can the user use the DA panel?
		boolean retval = false;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		Connection pcdb = null;
		Connection pcdb2 = null;
		Connection pcdb3 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		
		// Check direct user authorization
		if (au.getUid() == null || au.getUid().equalsIgnoreCase("")) {
			// No user ID.  We don't allow DA access without an explicit userid to audit.  Go away
			error(LOG,"Userid is not available, and thus, no DA access allowed");
			return false;
		} else {
			// Maybe the user is explicitly authorized -- check, with fall-thru
			try {
				pcdb2 = DatabaseConnectionFactory.getProconsulDatabaseConnection();
				if (pcdb2 != null) {
					ps2 = pcdb2.prepareStatement("select eppn from access_user where eppn = ? and type = 'da'");
					if (ps2 != null) {
						ps2.setString(1, au.getUid());
						rs2 = ps2.executeQuery();
						if (rs2 != null && rs2.next()) {
							log(LOG,"User " + au.getUid() + " is directly authorized to use DA privileges");
							return true;
						}
					}
				}
			} catch (Exception e) {
				return false; // if we except on checking, fail
			} finally {
				if (rs2 != null) {
					try {
						rs2.close();
					} catch (Exception ign) {
						// ignore
					}
				}
				if (ps2 != null) {
					try {
						ps2.close();
					} catch (Exception ign) {
						// ignore
					}
				}
				if (pcdb2 != null) {
					try {
						pcdb2.close();
					} catch (Exception ign) {
						// ignore
					}
				}
			}
		}
		// And check for an entitlement with fall-thru
		if (au.getEntitlements() == null || au.getEntitlements().isEmpty()) {
			// Nothing to see here
		} else {
			// Check entitlements for a match
			try {
				pcdb3 = DatabaseConnectionFactory.getProconsulDatabaseConnection();
				if (pcdb3 != null) {
					ps3 = pcdb3.prepareStatement("select entitlement from entitlements where entitlement = ? and type = 'da'");
					if (ps3 != null) {
						Iterator<String> iter = au.getEntitlements().iterator();
						while (retval == false && iter != null && iter.hasNext()) {
							String checkEntitlement = iter.next();
							ps3.setString(1,checkEntitlement);
							rs3 = ps3.executeQuery();
							if (rs3 != null && rs3.next()) {
								log(LOG,"DA authorized via entitlement " + checkEntitlement + " for user " + au.getUid());
								return true;
							}
						}
					}
				}
			} catch (Exception e) {
				return false;  // fail if we cannot check
			} finally {
				if (rs3 != null) {
					try {
						rs3.close();
					} catch (Exception ign) {
						// ignore
					}
				}
				if (ps3 != null) {
					try {
						ps3.close();
					} catch (Exception ign) {
						// ignore
					}
				}
				if (pcdb3 != null) {
					try {
						pcdb3.close();
					} catch (Exception ign) {
						// ignore
					}
				}
			}
		}
		// Check AuthUser memberships -- fail if we get here and don't get authorized.
		if (au.getMemberships() == null || au.getMemberships().isEmpty()) {
			log(LOG,"User " + au.getUid() +" has no memberships and thus is not a DA");
			return false;
		}
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select groupurn from access_groups where groupurn = ? and type = 'da'");
				if (ps != null) {
					Iterator<String> iter = au.getMemberships().iterator();
					while (retval == false && iter != null && iter.hasNext()) {
						String checkMembership = iter.next();
						ps.setString(1, checkMembership);
						debug(LOG,"DA check testing " + checkMembership);
						rs = ps.executeQuery();
						if (rs != null && rs.next()) {
							log(LOG,"DA authorized via " + checkMembership);
							return true; // if we find a group, we're in
						} else {
							debug(LOG,checkMembership + " does not authorize DA use");
						}
					}
					return false;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
		
	}
	public static boolean canUseApp(AuthUser au) {
		boolean retval = false;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		Connection pcdb = null;
		Connection pcdb2 = null;
		Connection pcdb3 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		
		// Check for explicit authorization by userid (with fall-thru)
		if (au.getUid() == null || au.getUid().equalsIgnoreCase("")) {
			// Fail if we don't have a uid value for the user for auditing purposes
			error(LOG,"Userid missing -- cannot proceed");
			return false;
		}
		try {
			pcdb2 = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb2 != null) {
				ps2 = pcdb2.prepareStatement("select eppn from access_user where eppn = ? and type = 'proconsul'");
				if (ps2 != null) {
					ps2.setString(1, au.getUid());
					rs2 = ps2.executeQuery();
					if (rs2 != null && rs2.next()) {
						log(LOG,"User " + au.getUid() + " is explicitly authorized to use Proconsul");
						return true;
					}
				}
			}
		} catch (Exception e) {
			return false; // fail if we are not able to test
		} finally {
			if (rs2 != null) {
				try {
					rs2.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps2 != null) {
				try {
					ps2.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (pcdb2 != null) {
				try {
					pcdb2.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
		// And check entitlements just in case (with fall-thru)
		if (au.getEntitlements() == null || au.getEntitlements().isEmpty()) {
			// Do nothing
		} else {
			try {
				pcdb3 = DatabaseConnectionFactory.getProconsulDatabaseConnection();
				if (pcdb3 != null) {
					ps3 = pcdb3.prepareStatement("select entitlement from entitlements where entitlement = ? and type = 'proconsul'");
					if (ps3 != null) {
						Iterator<String> iter = au.getEntitlements().iterator();
						while (retval == false && iter != null && iter.hasNext()) {
							String checkEntitlement = iter.next();
							ps3.setString(1,checkEntitlement);
							rs2 = ps3.executeQuery();
							if (rs3 != null && rs3.next()) {
								log(LOG,"User " + au.getUid() + " is authorized for Proconsul use via entitlement " + checkEntitlement);
								return true;
							}
						}
					}
				}
			} catch (Exception e) {
				return false;  // fail if we cannot check
			} finally {
				if (rs3 != null) {
					try {
						rs3.close();
					} catch (Exception ign) {
						//ignore
					}
				}
				if (ps3 != null) {
					try {
						ps3.close();
					} catch (Exception ign) {
						// ignore
					}
				}
				if (pcdb3 != null) {
					try {
						pcdb3.close();
					} catch (Exception ign) {
						// ignore
					}
				}
			}
		}
		// Check the AuthUser for a membership that confers app
		// access.  Only need one.
		if (au.getMemberships() == null || au.getMemberships().isEmpty()) {
			// no ticky, no washy
			error(LOG,"User " + au.getUid() + " has no memberships");
			return false;
		}
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select groupurn from access_groups where groupurn = ? and type = 'proconsul'");
				if (ps != null) {
					Iterator<String> iter = au.getMemberships().iterator();
					while (retval == false && iter != null && iter.hasNext()) {
						ps.setString(1, iter.next());
						rs = ps.executeQuery();
						if (rs.next()) {
							return true;
						}
					}
					return false;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	
	// Low-level check routines
	
	public static ArrayList<String> groupDNsForDelegate(AuthUser au) {
		// Given an authuser, determine the groups he or she is authorized to 
		// use for delegated administration and return an ArrayList of the values.
		// Essentially, for every ou in delegatedOusForUser(user) we add 
		// groupDnForOu(ou) to the list if it's not already there.
		ArrayList<String> retval = new ArrayList<String>();
		ArrayList<String> managedOUs = delegatedOusForUser(au);
		if (managedOUs == null || managedOUs.isEmpty()) {
			return null;  // null when we have none to return
		}
		for (String nextdn : managedOUs) {
			if (! retval.contains(groupDnForOu(nextdn))) {
				debug(LOG,"Adding groupDN " + groupDnForOu(nextdn) + " to list for ou " + nextdn);
				retval.add(groupDnForOu(nextdn));
			}
		}
		return retval;
	}
	public static ArrayList<String> delegatedOusForUser(AuthUser au) {
		// Return the list of ous the user is authorized to be a delegate over
		//
		//  Similar to canAdminFQDN, but without a target in mind
		//
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<String> retval = new ArrayList<String>();
		
		if (au.getMemberships() == null || au.getMemberships().isEmpty()) {
			// No groups, no admin
			error(LOG,"User " + au.getUid() + " is a member of no groups, so no delegation possible");
			return null;
		}
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select ou from delegate_groups where groupurn = ?");
				Iterator<String> iter = au.getMemberships().iterator();
				while (iter != null && iter.hasNext()) {
					// for every group the user is asserted to belong to
					ps.setString(1, iter.next());
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						// for every matched row...
						if (rs.getString("ou") != null) {
							debug(LOG,"Adding orgunit " + rs.getString("ou") + " to list for " + au.getUid());
							retval.add(rs.getString("ou")); // add the OU for this group
						}
					}
				}
			}
			return retval;
		} catch (Exception e) {
			// Return whatever we have
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	
	public static String groupDnForFQDN(String fqdn) {
		// Given an FQDN, return either null (if it has no delegate group) or the groupDN
		// for the AD group that manages the lowest superior OU in the tree.
		// This should be called AFTER determining that a user is a valid delegate for
		// the FQDN -- it simply chooses the lowest authority group with admin 
		// rights over an OU containing the FQDN.
		//
		// Get the OU for the FQDN, and walk it backwards
		//
		String retval = null;
		String checkOU = ouForFqdn(fqdn);
		while (checkOU != null && checkOU.contains(",") && retval == null) {
			retval = groupDnForOu(checkOU);
			if (retval == null) {
				checkOU = checkOU.replaceFirst("[^,]*,", ""); // strip and repeat
			} 
		}
		return retval;
	}
	
	public static String groupDnForOu(String ou) {
		// Given an OU identifier, return the group DN for delegated admins for that OU
		String retval = null;
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select groupdn from ou_group where upper(ou) = upper(?)");
				if (ps != null) {
					ps.setString(1, ou);
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						retval = rs.getString("groupdn");
					}
				}
			}
			return retval;
		} catch (Exception e) {
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	
	public static ArrayList<String> groupDnsForGroupUrn(String urn) {
		// Given a group URN, collect the set of group DNs it confers access to
		ArrayList<String> retval = new ArrayList<String>();
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select groupdn from group_group where upper(groupurn) = upper(?)");
				if (ps != null) {
					ps.setString(1, urn);
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						retval.add(rs.getString("groupdn"));
					}
				}
			}
			return retval;
		} catch (Exception e) {
			return(retval);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	
	public static ProconsulSession addGatewayToSession(ProconsulSession session) {
		String hostFqdn = session.getFqdn();
		String gatewayFqdn = session.getGatewayfqdn();
		if (gatewayFqdn == null) {
			gatewayFqdn = requiredGatewayFQDN(hostFqdn);
		}
		session.setGatewayfqdn(gatewayFqdn);
		return session;
	}
	
	public static String requiredGatewayFQDN(String fqdn) {
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		debug(LOG,"Checking gateway requirement for " + fqdn);
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select gateway from host_gateway where upper(fqdn) = upper(?)");
				ps.setString(1, fqdn);
				rs = ps.executeQuery();
				if (rs == null || ! rs.next()) {
					// No gateway required -- return null;
					return null;
				} else {
					return rs.getString("gateway");
				}
			} else {
				return null;  // null if we can't get into the database
			}
		} catch (Exception e) {
			return null;  // no gateway if we can't get a gateway
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (ps != null) {
					ps.close();
				}
				if (pcdb != null) {
					pcdb.close();
				}
			} catch (Exception ign) {
				// ignore
			}
		}
	}
	
	public static ADUser addGatewayAccessGroup(ADUser u, String fqdn) {
		// Given an ADUser and a host, if the host requires a gateway, add the group for the gateway to 
		// the user.
		// 
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		debug(LOG,"addGatewayAccessGroup starting");
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select groupdn from host_gateway where upper(fqdn) = upper(?)");
				ps.setString(1, fqdn);
				rs = ps.executeQuery();
				if (rs == null || ! rs.next()) {
					return u;  // no change
				} else {
					debug(LOG,"Adding gateway access group");
					String groupToAdd = rs.getString("groupdn");
					return addGroupToADUser(groupToAdd,u);
				}
			} else {
				return u;  // default back to the original user
			}
		} catch (Exception e) {
			return u; // just return user in this case
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (ps != null) {
					ps.close();
				}
				if (pcdb != null) {
					pcdb.close();
				}
			} catch (Exception ign) {
				// ignore
			}
		}
	}
	
	public static ADUser addHostAccessGroup(ADUser u, String fqdn) {
		// given an ADUser and a host to access, add the group needed to provide access
		// if one exists.
		//
		// First, find the appropriate group if it exists
		//
		Connection pcdb = null;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		debug(LOG,"addHostAccessGroup starting");
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select groupdn from host_access_group where upper(fqdn) = upper(?)");
				ps.setString(1,  fqdn);
				rs = ps.executeQuery();
				debug(LOG,"First addHostAccessGroup query done");
				if (rs == null || ! rs.next()) {
					ps2 = pcdb.prepareStatement("select groupdn from host_access_group where upper(?) like concat('%',upper(ou))");
					String tou = ouForFqdn(fqdn);
					if (tou != null) {
						ps2.setString(1, tou);
						rs2 = ps2.executeQuery();
						debug(LOG,"Second addHostAccessGroup query done");
					} 
					if (rs2 == null || ! rs2.next()) {
						// Just return the incoming user
						debug(LOG,"Returning without adding host access group");
						return u;
					} else {
						// Add the group
						debug(LOG,"Adding host access group (2)");
						String groupToAdd = rs2.getString("groupdn");
						debug(LOG,"Host access group(2) added");
						return addGroupToADUser(groupToAdd,u);
					}
				} else {
					// Add the group
					debug(LOG,"Adding host access group");
					String addGroup = rs.getString("groupdn");
					debug(LOG,"Host access group added");
					return addGroupToADUser(addGroup,u);
				}
			} else {
				return u;  // if not db connection, just return the input user
			}
		} catch (Exception e) {
			return u;  // on exception, just return the input
		} finally {
			if (rs2 != null) {
				try {
					rs2.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps2 != null) {
				try {
					ps2.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	
	public static ArrayList<String> groupDnsForEppn(String eppn) {
		// Given an eppn value, find all the group DNs explicitly assigned to it
		ArrayList<String> retval = new ArrayList<String>();
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select groupdn from explicit_groups where upper(eppn) = upper(?)");
				if (ps != null) {
					ps.setString(1, eppn);
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						retval.add(rs.getString("groupdn"));
					}
				}
			}
			return retval;
		} catch (Exception e) {
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	
	public static ArrayList<String> groupDnsForEntitlement(String entitlement) {
		// Given an entitlement value, find any group DNs that it confers access to
		//
		ArrayList<String> retval = new ArrayList<String>();
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select groupdn from entitlement_groups where upper(entitlement) = upper(?)");
				if (ps != null) {
					ps.setString(1, entitlement);
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						retval.add(rs.getString("groupdn"));
					}
				}
			}
			return retval;
		} catch (Exception e) {
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	
	
	public static String ouForFqdn(String fqdn) {
		// Given the FQDN of a host, return either null (if it cannot be found) or
		// the OU the FQDN resides in in the AD if it can be found
		//
		String retval = null;
		DirContext dc = null;
		PCConfig config = PCConfig.getInstance();
		NamingEnumeration<SearchResult> rs = null;
		debug(LOG,"ouForFQDN starting");
		try {
			dc = LDAPConnectionFactory.getAdminConnection();
			SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String basedn = config.getProperty("ldap.searchbase", true);
			debug(LOG,"Querying for dnshostname");
			rs = dc.search(basedn,"(&(objectcategory=computer)(dnshostname="+fqdn+"))",sc);
			debug(LOG,"Query for dnshostname complete");
			while (rs != null && rs.hasMore()) {
				String dn = rs.next().getNameInNamespace();
				retval = dn.replaceFirst("[^,]*,", "");
			}
		} catch (Exception e) {
			// ignore
		}
		return retval;
	}
	
	public static ArrayList<String> fqdnsForOu(String ou) {
		// Given the DN of an OU in the AD, return the list of host FQDNs under it
		//
		ArrayList<String> retval = new ArrayList<String>();
		
		DirContext dc = null;
		NamingEnumeration<SearchResult> rs = null;
		
		try {
			dc = LDAPConnectionFactory.getAdminConnection();
			SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String[] attrs = {"dnshostname"};
			sc.setReturningAttributes(attrs);;
			rs = dc.search(ou, "objectclass=computer",sc);
			
			while (rs != null && rs.hasMore()) {
				Attribute dh = rs.next().getAttributes().get("dnshostname");
				if (dh != null) {
					retval.add(((String) dh.get()).toLowerCase());
				}
			}
			debug(LOG,"fqdnsForOu found " + retval.size() + "values");
			return retval;
		} catch (Exception e) {
			error(LOG,"Exception: " + e.getMessage() + " during fqdnsforou");
			return retval;
		}
	}
	
	public static ArrayList<String> fqdnsForGroupUrn(String urn) {
		// Given an authuser group URN, get the list of fqdns it affords access to
		ArrayList<String> retval = new ArrayList<String>();
		
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select fqdn,ou from group_host where upper(groupurn) = upper(?)");
				if (ps != null) {
					ps.setString(1, urn);
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						if (rs.getString("fqdn") != null && ! rs.getString("fqdn").equals("")) {
							// This is an fqdn delegation
							retval.add(rs.getString("fqdn"));
						}
						if (rs.getString("ou") != null && ! rs.getString("ou").equals("")) {
							// 	This is an ou delegation
							retval.addAll(fqdnsForOu(rs.getString("ou")));
						}
					}
				}
			}
			return retval;
		} catch (Exception e) {
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	
	public static ArrayList<String> fqdnsForStatic(String eppn) {
		ArrayList<String> retval = new ArrayList<String>();
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select fqdn from static_host where upper(eppn) = upper(?)");
				if (ps != null) {
					ps.setString(1, eppn);
				
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						if (rs.getString("fqdn") != null && ! rs.getString("fqdn").equals("")) {
								// 	direct delegation
								retval.add(rs.getString("fqdn"));
						}
					}
				}
			}
			debug(LOG,"fqdnsForStatic found " + retval.size() + " values");
			return retval;
		} catch (Exception e) {
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
	}
	
	public static ArrayList<String> fqdnsForEppn(String eppn) {
		ArrayList<String> retval = new ArrayList<String>();
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select fqdn,ou from explicit_hosts where upper(eppn) = upper(?)");
				if (ps != null) {
					ps.setString(1, eppn);
				
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						if (rs.getString("fqdn") != null && ! rs.getString("fqdn").equals("")) {
								// 	direct delegation
								retval.add(rs.getString("fqdn"));
						}
						if (rs.getString("ou") != null && ! rs.getString("ou").equals("")) {
							// 	by-ou delegation
							retval.addAll(fqdnsForOu(rs.getString("ou")));
							}
					}
				}
			}
			debug(LOG,"fqdnsForEppn found " + retval.size() + " values");
			return retval;
		} catch (Exception e) {
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
	}
	
	public static ArrayList<String> fqdnsForEntitlement(String entitlement) {
		ArrayList<String> retval = new ArrayList<String>();
		
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {	
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select fqdn,ou from entitlement_host where upper(entitlement) = upper(?)");
				if (ps != null) {
					ps.setString(1, entitlement);
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						if (rs.getString("fqdn") != null && ! rs.getString("fqdn").equals("")) {
							retval.add(rs.getString("fqdn"));
						}
						if (rs.getString("ou") != null && ! rs.getString("ou").equals("")) {
							retval.addAll(fqdnsForOu(rs.getString("ou")));
						}
					}
				}
			}
			return retval;
		} catch (Exception e) {
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	public static boolean makeUserAuthorizedDA(ADUser au) {
		return makeUserAuthorizedDA(au, null, null);
	}
	public static boolean makeUserAuthorizedDA(ADUser au, AuthUser authu, String clientip) {
		// Add the specified user to the database's active_domain_admins table
		Connection pcdb = null;
		PreparedStatement ps = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("insert into active_domain_admins values(?,?,?)");
				if (ps != null) {
					ps.setString(1, au.getsAMAccountName());
					ps.setInt(2,(int) (System.currentTimeMillis()/1000));
					ps.setInt(3, 0);
					ps.executeUpdate();
					if (authu != null)
						audit(LOG,"makeActiveDA",authu.getUid(),null,au.getsAMAccountName(),null,clientip);
					else
						audit(LOG,"makeActiveDA",null,null,au.getsAMAccountName(),null,null);
					return true;
				} else {
					error(LOG,"Failed inserting " + au.getsAMAccountName() + " into active domain admins table");
					return false;
				}
			} else {
				error(LOG,"Failed inserting " + au.getsAMAccountName() + " into active_domain_admins table");
				return false;
			}
			
		} catch (Exception e) {
			error(LOG,"Caught exception " + e.getMessage() + " during attempt to write to active_domain_admins table");
			return false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	public static boolean removeAuthorizedDA(ADUser au) {
		return removeAuthorizedDA(au,null,null);
	}
	public static boolean removeAuthorizedDA(ADUser au, AuthUser authu, String clientip) {
		// Remove the specified samaccountname from the database for active ad's
		Connection pcdb=null;
		PreparedStatement ps=null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("update active_domain_admins set disabletime = ? where samaccountname = ?");
				if (ps != null) {
					ps.setInt(1,(int) (System.currentTimeMillis()/1000));
					ps.setString(2, au.getsAMAccountName());
					ps.executeUpdate();
					if (authu != null)
						audit(LOG, "removeActiveDA",authu.getUid(),null,au.getsAMAccountName(),null,clientip);
					else
						audit(LOG,"removeActiveDA",null,null,au.getsAMAccountName(),null,null);
					return true;
				} else {
					error(LOG,"failed to remove " + au.getsAMAccountName() + " from active_domain_admins table");
					return false;
				}
			} else {
				error(LOG,"Failed to remove " + au.getsAMAccountName() + " from active_domain_admins table");
				return false;
			}
		} catch (Exception e) {
			error(LOG,"Caught exception " + e.getMessage() + " while removing " + au.getsAMAccountName() + " from active_domain_admins table");
			return false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	public static ArrayList<String> getDomainAdminFQDNs() {
		// Return the list of hosts authorized to be used as domain admin targets
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<String> retval = new ArrayList<String>();
		debug(LOG,"Getting DomAdmin FQDN values");
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select fqdn from domain_admin_hosts");
				if (ps != null) {
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						retval.add(rs.getString("fqdn"));
					}
				}
			}
			return retval;
		} catch (Exception e) {
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
	}
	
	public static ArrayList<DisplayFQDN> getAvailableSessionDisplayFQDNs(AuthUser au, String stype) {
		// Given an authenticated user, get back just the list of FQDNs from his active
		// sessions.  Shortcut for initializating session resumption display.
		//
		String myUser = au.getUid();
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<DisplayFQDN> retval = new ArrayList<DisplayFQDN>();
		debug(LOG,"Getting Session FQDN values");
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				if (stype != null && ! stype.equals("")) {
					ps = pcdb.prepareStatement("select fqdn,displayname,delegatedou,delegatedrole,type from sessions where upper(owner) = upper(?) and upper(type) = upper(?)");
					if (ps != null) {
						ps.setString(2, stype);
					}
				} else {
					ps = pcdb.prepareStatement("select fqdn,displayname,delegatedou,delegatedrole,type from sessions where upper(owner) = upper(?)");
				}
				// in either case...
				if (ps != null) {
					ps.setString(1, myUser);
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						DisplayFQDN adder = new DisplayFQDN(rs.getString("fqdn"),rs.getString("displayname"));
						if (rs.getString("delegatedou") != null) {
							adder.setDelegatedOU(rs.getString("delegatedou"));
						}
						if (rs.getString("delegatedrole") != null) {
							adder.setDelegatedRole(rs.getString("delegatedrole"));
						}
						retval.add(adder);
					}
				}
			}
			return retval;
		} catch (Exception e) {
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
	}
	
	public static void writeAuditLog(String eppn, String event, String targetuser, String targethost, String targetgroup, String clientip) {
		Connection pcdb = null;
		PreparedStatement ps = null;
		debug(LOG,"Called writeAuditLog");
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("insert into audit_log values(?,?,?,?,?,?,?)");
				if (ps != null) {
					ps.setString(1, new SimpleDateFormat("yyyy-MM-dd kk:mm:ss").format(new Date()));
					if (eppn == null) {
					ps.setNull(2, java.sql.Types.VARCHAR);
					} else {
						ps.setString(2,eppn);
					}
					if (event != null) {
					ps.setString(3, event);
					} else {
						ps.setNull(3,  java.sql.Types.VARCHAR);
					}
					if (targetuser != null) {
					ps.setString(4, targetuser);
					} else {
						ps.setNull(4, java.sql.Types.VARCHAR);
					}
					if (targethost != null) {
					ps.setString(5,  targethost);
					} else {
						ps.setNull(5,  java.sql.Types.VARCHAR);
					}
					if (targetgroup != null) {
					ps.setString(6,  targetgroup);
					} else {
						ps.setNull(6,  java.sql.Types.VARCHAR);
					}
					if (clientip != null) {
					ps.setString(7, clientip);
					} else {
						ps.setNull(7,  java.sql.Types.VARCHAR);
					}
					ps.executeUpdate();
				}
			}
		} catch (Exception e) {
			// ignore but log
			error(LOG,"Audit logging threw exception: " + e.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
	}
	
	public static ArrayList<CheckedOutCred> getAvailableCheckedOutUsers(AuthUser au) {
		String myUser = au.getUid();
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<CheckedOutCred> retval = new ArrayList<CheckedOutCred>();
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select * from checkout where upper(owner) = upper(?) and expiretime >= ?");
				if (ps != null) {
					ps.setString(1, myUser);
					ps.setLong(2, System.currentTimeMillis());
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						CheckedOutCred cc = new CheckedOutCred();
						AuthUser cauth = new AuthUser();
						cauth.setUid(rs.getString("owner"));
						cc.setAuthUser(cauth);
						cc.setStartTime(new Date(rs.getLong("starttime")));
						cc.setStatus("active");
						cc.setExpirationTime(rs.getLong("expiretime"));
						cc.setLifetime((int) (cc.getExpirationTime() - cc.getStartTime().getTime()) / (60*60*1000));
						cc.setExpirationDate((new Date(cc.getExpirationTime())).toLocaleString());
						cc.setReason(rs.getString("reason"));
						cc.setTargetHost(rs.getString("targethost"));
						ADUser cad = new ADUser();
						cad.setsAMAccountName(rs.getString("aduser"));
						cc.setTargetUser(cad);
						retval.add(cc);
					}
				}
			}
			return retval;
		} catch (Exception e) {
			error(LOG,"Threw exception: " + e.getMessage() + "geting list of avaialble checked out creds");
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
	}
	
	public static ArrayList<ProconsulSession> getAvailableSessions(AuthUser au) {
		// Given an authenticated user, return an ArrayList of session objects
		// describing the sessions the user has currently available
		//
		String myUser = au.getUid();
		Connection pcdb = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<ProconsulSession> retval = new ArrayList<ProconsulSession>();
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select * from sessions where upper(owner) = upper(?)");
				if (ps != null) {
					ps.setString(1, myUser);
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						ProconsulSession psess = new ProconsulSession(au,rs.getString("fqdn"),rs.getString("vncpassword"), LDAPUtils.loadAdUser(rs.getString("samaccountname")), rs.getString("displayname"));
						// Sessions have become more complex, and we need to import the status, the novncport, and the gateway FQDN
						if (rs.getString("status") != null)
							psess.setStatus(Status.valueOf(rs.getString("status").toUpperCase()));
						if (rs.getString("novncport") != null) 
							psess.setNovncPort(Integer.parseInt(rs.getString("novncport")));
						if (rs.getString("gatewayfqdn") != null) {
							psess.setGatewayfqdn(rs.getString("gatewayfqdn"));
						}
						retval.add(psess);
					}
				}
			}
			return retval;
		} catch (Exception e) {
			error(LOG,"Threw exception: " + e.getMessage() + " getting list of available sesssions");
			return retval;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					// ignore
				}
			}
		}
	}
	
	public static String getADDomainAdminGroupDn() {
		// Read the configuration and determine what group should be used for 
		// the domain admins interface.  This is primarily for testing purposes, where
		// it's important not to be changing the real domain admins group, but may be
		// useful for cases in which a site wants to use a non-standard admin group for
		// this purpose.
		
		PCConfig config = PCConfig.getInstance();
		String retval = null;
		retval = config.getProperty("ldap.dagroup", true);
		return retval;
	}
	
	public static boolean extendCheckedOutUser(CheckedOutCred co) {
		return extendCheckedOutUser(co, null);
	}
	public static boolean extendCheckedOutUser(CheckedOutCred co, String clientip) {
		Connection pcdb = null;
		PreparedStatement ps = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("update checkout set expiretime = expiretime + ? where owner = ? and aduser = ? and expiretime >= ?");
				if (ps != null) {
					ps.setLong(1, 2*60*60*1000);
					ps.setString(2, co.getAuthUser().getUid());
					ps.setString(3, co.getTargetUser().getsAMAccountName());
					ps.setLong(4, System.currentTimeMillis());
					ps.executeUpdate();
					audit(LOG,"extendCheckedOutCredLifetime",co.getAuthUser().getUid(),co.getTargetHost(),co.getTargetUser().getsAMAccountName(),null,clientip);
					return true;
				} else {
					error(LOG,"Failed to extend lifetime of checked out credential " + co.getTargetUser().getsAMAccountName() + " for " + co.getAuthUser().getUid());
					return false;
				}
			} else {
				error(LOG,"Failed to extend lifetime of checked out credential " + co.getTargetUser().getsAMAccountName() + " for " + co.getAuthUser().getUid());
				return false;
			}
		} catch (Exception e) {
			error(LOG,"Caught exception " + e.getMessage() + " when extending lifetime of " + co.getTargetUser().getsAMAccountName() + " for " + co.getAuthUser().getUid());
			return false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	
	public static boolean expireCheckedOutUser(CheckedOutCred co) {
		return expireCheckedOutUser(co, null);
	}
	public static boolean expireCheckedOutUser(CheckedOutCred co, String clientip) {
		Connection pcdb = null;
		PreparedStatement ps = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("update checkout set expiretime = ? where owner = ? and aduser = ? and expiretime >= ?");
				if (ps != null) {
					ps.setLong(1, System.currentTimeMillis());
					ps.setString(2, co.getAuthUser().getUid());
					ps.setString(3, co.getTargetUser().getsAMAccountName());
					ps.setLong(4, System.currentTimeMillis());
					ps.executeUpdate();
					audit(LOG,"expireCheckedOutCred",co.getAuthUser().getUid(),co.getTargetHost(),co.getTargetUser().getsAMAccountName(),null,clientip);
					return true;
				} else {
					error(LOG,"Failed to early-expire checked out user " + co.getTargetUser().getsAMAccountName());
					return false;
				}
			} else {
				error(LOG,"Failed to early-expire checked out user " + co.getTargetUser().getsAMAccountName());
				return false;
			}
		} catch (Exception e) {
			error(LOG,"Caught exception " + e.getMessage() + " while early-expiring " + co.getTargetUser().getsAMAccountName());
			return false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
	}
	
	public static boolean storeCheckedOutUser(CheckedOutCred co) {
		Connection pcdb = null;
		PreparedStatement ps = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("insert into checkout values(?,?,?,?,?,?,?)");
				if (ps != null) {
					ps.setString(1,co.getAuthUser().getUid()); 
					ps.setLong(2,co.getStartTime().getTime());
					ps.setLong(3, co.getExpirationTime());
					ps.setString(4, co.getTargetHost());
					ps.setString(5, co.getReason());
					ps.setString(6, co.getTargetUser().getsAMAccountName());
					ps.setString(7,  "active"); // creation state -- only become inactive after deletion from the AD
					
					ps.executeUpdate();
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
			
		} catch (Exception e) {
			return false;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}	
	}
}
