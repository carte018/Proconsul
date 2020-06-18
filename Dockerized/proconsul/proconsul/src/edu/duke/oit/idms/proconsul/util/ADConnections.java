package edu.duke.oit.idms.proconsul.util;

import java.util.ArrayList;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

//import com.sun.istack.internal.logging.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.duke.oit.idms.proconsul.cfg.PCConfig;

/**
 * ldap connections to all the AD DCs in an arraylist using admin pricnipal and 
 * connection pooling
 * 
 * @author rob
 *
 */
public class ADConnections {
	private static final Logger LOG = LoggerFactory.getLogger(ADConnections.class);

	public ArrayList<LDAPAdminConnection> connections = null;
	public ADConnections() {
		PCConfig config = PCConfig.getInstance();
		String adurls = null;
		//String adurls = config.getProperty("ldap.dcs", true);
		
		// First, attempt to infer the right list of domain controllers
		// Make an administrative connection to the domain name (ldap.provider) from the config
		//
		DirContext dc = null;
		NamingEnumeration<SearchResult> results = null;
		try {
			dc = LDAPConnectionFactory.getAdminConnection();
			
			if (dc != null) {
				SearchControls sc = new SearchControls();
				sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
				sc.setReturningAttributes(new String[] {"dnsHostName","serverReferenceBL"});
				String searchBase = config.getProperty("ldap.searchbase", true);
				String localSite = config.getProperty("ldap.siteDN",false);
				if (localSite != null) {
					//localSite = ".*" + localSite + ".*";
					// localSite = ".*=" + localSite + ",.*"; // restrict to just the local site - no subsites
					localSite = ".*=" + localSite + "[-,].*"; // we now have US-RDU-subsite sites to cope with
				}
				results = dc.search(searchBase,"(&(objectCategory=computer)(userAccountControl:1.2.840.113556.1.4.803:=8192))",sc);
				
				while (results != null && results.hasMore()) {
					SearchResult sr = results.next();
					if (localSite != null && sr.getAttributes().get("serverReferenceBL").get().toString().matches(localSite)) { 
						String dcfqdn = (String) sr.getAttributes().get("dnsHostName").get();
						ProconsulUtils.debug(LOG,"Adding " + dcfqdn + " to DC list");
						if (adurls == null) {
							adurls = "ldaps://"+dcfqdn+":636";
						} else {
							adurls = adurls + "," + "ldaps://"+dcfqdn+":636";
						}
					} else {
						ProconsulUtils.debug(LOG,"Discarding " + sr.getAttributes().get("dnsHostName").get() + " on no match to reference BL " + sr.getAttributes().get("serverReferenceBL").get());
					}
				}
				ProconsulUtils.debug(LOG,"adurls after DNS search is " + adurls);
			} else {
				ProconsulUtils.debug(LOG,"adurls cannot be set from AD search due to missing AD connection");
			}
		} catch (Exception e) {
			ProconsulUtils.error(LOG,"Exception thrown during LDAP search for DCs - " + e.getMessage());
			// ignore
		} finally {
			if (results != null) {
				try {
					results.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (dc != null) {
				try {
					dc.close();
				} catch (Exception ign) {
					//ignore
				}
			}
		}
		
		if (adurls == null) {
			adurls = config.getProperty("ldap.dcs", true);
		}
		
		ProconsulUtils.debug(LOG,"adurls after property get is " + adurls);
		
		connections = new ArrayList<LDAPAdminConnection>();
		
		String[] urlarray = adurls.split(",");
		
		for (int i = 0; i < urlarray.length; i++) {
			String target = urlarray[i];
			
			connections.add(LDAPAdminConnection.getInstance(target));
			ProconsulUtils.debug(LOG,"Added connection to " + target);
		}
	}
	public void close() {
		// Call the close() method for every connection in the array
		
		for (LDAPAdminConnection revoke : connections) {
			try {
				if (revoke != null && revoke.getConnection() != null) {
					revoke.getConnection().close();
				}
			} catch (Exception e) {
				// ignore
			}
		}			
	}
	
}

