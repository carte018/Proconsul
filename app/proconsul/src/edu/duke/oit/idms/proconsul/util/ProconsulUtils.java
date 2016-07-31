package edu.duke.oit.idms.proconsul.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import javax.naming.NamingEnumeration;
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

import com.sun.istack.internal.logging.Logger;

import edu.duke.oit.idms.proconsul.DisplayFQDN;
import edu.duke.oit.idms.proconsul.cfg.PCConfig;

import java.util.Date;
import java.text.SimpleDateFormat;


public class ProconsulUtils {

	// Static class for utilities
	
	private static final Logger LOG = Logger.getLogger(ProconsulUtils.class);
	
	public static boolean isSamaccountnameAvailable(String sam, LDAPAdminConnection lac) {
		DirContext dc = null;
		NamingEnumeration<SearchResult> result = null;
		try {
			dc = lac.getConnection();
			if (dc == null || sam == null || sam.equals("")) {
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
	
	public static boolean isSamaccountnameAvailable(String sam) {
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
			if (results == null || ! results.hasMore()) {
				return true;
			} else {
				return false;
			}
			
		} catch (Exception e) {
			throw new RuntimeException("Failed looking up samaccountname in AD");
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
	
	// Static routine to find an unused random sAMAccountName value
	public static String getRandomSamaccountname() {
		String retval = null;
		int count = 0;
		while (! ProconsulUtils.isSamaccountnameAvailable(retval) && count++ < 10) {
			// 10 retries before failing
			retval = RandomStringUtils.randomAlphanumeric(9);
			retval += "-eas";
		}
		if (count < 10) {
			return retval;
		} else {
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
			writeAuditLog(null,"ADuserDelete",u.getsAMAccountName(),null,null,null);
			LOG.info("Deleted ad user " + u.getsAMAccountName());
			return u;
		} catch (Exception e) {
			return null;
		}
	}
	
	//Statically create an AD object from an ADUser object
	//Because of possible AD propagation delays, we use 
	//a collection of connections and force updates across them
	//all.
	
	public static ADUser createAdUser(ADUser u) {
		if (u == null || u.getsAMAccountName() == null || u.getsAMAccountName().equals("") || u.getAdDomain() == null || u.getAdDomain().equals("")) {
			LOG.info("createAdUser: empty samaccountname");
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
			LOG.info("createAdUser: Search scope creation failed");
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
						LOG.info("Initial account creation completed");
						userCreated = true;
					} else {
							while (isSamaccountnameAvailable(u.getsAMAccountName(),lac)) {
								LOG.info("Waiting for propagation again...");
								// blocking loop to wait for propagation
							}
							LOG.info("Propagation wins!");
					}
				} catch (Exception ign) {
					LOG.info("Threw " + ign.getMessage() + " while creating AD user - " + u.getsAMAccountName());
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
					LOG.info("Threw " + ign2.getMessage() + " changing password for user");
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
					LOG.info("Threw " + ign3.getMessage() + " during require password update");
					//ignore
				}
			}
			// Write out an audit log for what we just did
			writeAuditLog(null,"createADUser",u.getsAMAccountName(),null,null,null);
		} catch (Exception e) {
			LOG.info("AD User creation threw exception: " + e.getMessage());
			throw new RuntimeException(e);
		}
		u.setCreated(true);
		LOG.info("AD User creation succeeded");
		return u;
	}
	
	// Set a new random password for an existing AD user
	public static ADUser setRandomADPassword(ADUser adu) {
		String password = null;
		PCConfig config = PCConfig.getInstance();
		try {
			password = ProconsulUtils.getRandomPassword();
		} catch (Exception e) {
			LOG.info("Unable to set random password -- returning original ADUser");
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
					LOG.info("Unable to find AD user during password randomization");
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
						LOG.info("Threw " + ign2.getMessage() + " during randomization of password");
						//ignore
					}
				}
				adu.setAdPassword(password);
				return adu;
			} else {
				return adu;
			}
			
		} catch (Exception e) {
			LOG.info("Failed setting random password in AD -- returning original ADUser");
			return adu;
		}
		
	}
	
	// Create an ADUser using random sAMAccountName and password
	public static ADUser createRandomizedADUser() {
		String userid = null;
		String password = null;
		try {
			userid = ProconsulUtils.getRandomSamaccountname();
			password = ProconsulUtils.getRandomPassword();
		} catch (Exception e) {
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
		
	
	// VNC Port Number acquisition.  We arrange to encumber the 
	// port number in the database before we return it.  
	// Session data is used to encumber the number
	
	public static int getVncPortNumber(ProconsulSession sess) {
		// Find an (even-numbered) unused port by +2-ing the highest
		// numbered port in the database and rely on attrition to
		// eventually free up intervening ports.
		
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
			
			if (rs == null || ! rs.next()) {
				LOG.info("Could not find session for " + sess.getOwner().getUid() + "," + sess.getFqdn() + "," + sess.getTargetUser().getsAMAccountName());
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
				writeAuditLog(log_eppn,"startSession",log_targetuser,log_targethost,null,null);
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
					writeAuditLog(log_eppn,"starting",log_targetuser,sess.getFqdn(),null,null);
					break;
				case CONNECTED:
					writeAuditLog(log_eppn,"connected",log_targetuser,sess.getFqdn(),null,null);
					break;
				case DISCONNECTED:
					writeAuditLog(log_eppn,"disconnected",log_targetuser,sess.getFqdn(),null,null);
					break;
				case TERMINATING:
					writeAuditLog(log_eppn,"terminating",log_targetuser,sess.getFqdn(),null,null);
					break;
				}
				return;
			}
		} catch (Exception e) {
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
				throw new RuntimeException("Failed database connection deleting port number " + portnum);
			}
		} catch (Exception e) {
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
				throw new RuntimeException("Failed database connection deleting session");
			}
		} catch (Exception e) {
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
	
	// Add a group specified by a URN to a user identified by an ADUser object
	public static ADUser addGroupToADUser(String urn, ADUser u) {
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
					LOG.info("Added " + groupDN + " membership");
					writeAuditLog(null,"addGroup",u.getsAMAccountName(),null,urn,null);
				} catch (Exception ign) {
					LOG.info("Threw exception " + ign.getMessage() + " on addGroup for " + u.getsAMAccountName() + "," + urn);
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
			LOG.info("User " + au.getUid() + " has no membership and thus is not a delegate");
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
			LOG.info("Failed to validate delegated admin rights for " + fqdn + "by user " + au.getUid() + " due to exception " + e.getMessage());
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
	public static boolean canUseDA(AuthUser au) {
		// Can the user use the DA panel?
		boolean retval = false;
		PreparedStatement ps = null;
		Connection pcdb = null;
		ResultSet rs = null;
		// Check AuthUser memberships
		if (au.getMemberships() == null || au.getMemberships().isEmpty()) {
			LOG.info("User " + au.getUid() +" has no memberships and thus is not a DA");
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
						LOG.info("DA check testing " + checkMembership);
						rs = ps.executeQuery();
						if (rs != null && rs.next()) {
							LOG.info("DA authorized via " + checkMembership);
							return true; // if we find a group, we're in
						} else {
							LOG.info(checkMembership + " does not authorize DA use");
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
		Connection pcdb = null;
		ResultSet rs = null;
		
		// Check the AuthUser for a membership that confers app
		// access.  Only need one.
		if (au.getMemberships() == null || au.getMemberships().isEmpty()) {
			// no ticky, no washy
			LOG.info("User " + au.getUid() + " has no memberships");
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
				LOG.info("Adding groupDN " + groupDnForOu(nextdn) + " to list for ou " + nextdn);
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
			LOG.info("User " + au.getUid() + " is a member of no groups, so no delegation possible");
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
							LOG.info("Adding orgunit " + rs.getString("ou") + " to list for " + au.getUid());
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
		LOG.info("Checking gateway requirement for " + fqdn);
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
		LOG.info("addGatewayAccessGroup starting");
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select groupdn from host_gateway where upper(fqdn) = upper(?)");
				ps.setString(1, fqdn);
				rs = ps.executeQuery();
				if (rs == null || ! rs.next()) {
					return u;  // no change
				} else {
					LOG.info("Adding gateway access group");
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
		LOG.info("addHostAccessGroup starting");
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select groupdn from host_access_group where upper(fqdn) = upper(?)");
				ps.setString(1,  fqdn);
				rs = ps.executeQuery();
				LOG.info("First addHostAccessGroup query done");
				if (rs == null || ! rs.next()) {
					ps2 = pcdb.prepareStatement("select groupdn from host_access_group where upper(?) like concat('%',upper(ou))");
					String tou = ouForFqdn(fqdn);
					if (tou != null) {
						ps2.setString(1, tou);
						rs2 = ps2.executeQuery();
						LOG.info("Second addHostAccessGroup query done");
					} 
					if (rs2 == null || ! rs2.next()) {
						// Just return the incoming user
						LOG.info("Returning without adding host access group");
						return u;
					} else {
						// Add the group
						LOG.info("Adding host access group (2)");
						String groupToAdd = rs2.getString("groupdn");
						LOG.info("Host access group(2) added");
						return addGroupToADUser(groupToAdd,u);
					}
				} else {
					// Add the group
					LOG.info("Adding host access group");
					String addGroup = rs.getString("groupdn");
					LOG.info("Host access group added");
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
		LOG.info("ouForFQDN starting");
		try {
			dc = LDAPConnectionFactory.getAdminConnection();
			SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String basedn = config.getProperty("ldap.searchbase", true);
			LOG.info("Querying for dnshostname");
			rs = dc.search(basedn,"(&(objectcategory=computer)(dnshostname="+fqdn+"))",sc);
			LOG.info("Query for dnshostname complete");
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
			LOG.info("fqdnsForOu found " + retval.size() + "values");
			return retval;
		} catch (Exception e) {
			LOG.info("Exception: " + e.getMessage() + " during fqdnsforou");
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
			LOG.info("fqdnsForEppn found " + retval.size() + " values");
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
		// Add the specified user to the database's active_domain_admins table
		Connection pcdb = null;
		PreparedStatement ps = null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("insert into active_domain_admins values(?)");
				if (ps != null) {
					ps.setString(1, au.getsAMAccountName());
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
	public static boolean removeAuthorizedDA(ADUser au) {
		// Remove the specified samaccountname from the database for active ad's
		Connection pcdb=null;
		PreparedStatement ps=null;
		
		try {
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("delete from active_domain_admins where samaccountname = ?");
				if (ps != null) {
					ps.setString(1, au.getsAMAccountName());
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
		LOG.info("Getting DomAdmin FQDN values");
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
		LOG.info("Getting Session FQDN values");
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
		LOG.info("Called writeAuditLog");
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
			LOG.info("Creating AD user threw exception: " + e.getMessage());
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
						retval.add(psess);
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
}
