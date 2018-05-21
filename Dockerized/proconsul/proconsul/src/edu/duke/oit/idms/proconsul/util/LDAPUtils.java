package edu.duke.oit.idms.proconsul.util;

import java.util.ArrayList;

import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import edu.duke.oit.idms.proconsul.cfg.PCConfig;

public class LDAPUtils {
	// Static methods for handling LDAP operations 
	
	public static ArrayList<String> getMemberships(String sam) {
		// Given a sAMAccountName, return an ArrayList of the group DNs
		// sam is a member of.
		ArrayList<String> retval = new ArrayList<String>();
		DirContext dc = null;
		NamingEnumeration<SearchResult> results = null;
		try {
			dc = LDAPConnectionFactory.getAdminConnection();
			if (dc == null || sam == null || sam.equals("")) {
				return retval;
			} else {
				SearchControls sc = new SearchControls();
				sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
				sc.setReturningAttributes(new String[] {"memberOf"});
				PCConfig config = PCConfig.getInstance();
				
				String searchBase = config.getProperty("ldap.searchbase", true);
				
				results = dc.search(searchBase,"samaccountname="+sam,sc);
				while (results != null && results.hasMore()) {
					NamingEnumeration<?> e =  results.next().getAttributes().get("memberOf").getAll();
					while (e.hasMore()) {
						retval.add((String) e.next().toString());
					}
				}
				return retval;
			}
		} catch (Exception e) {
			return retval;
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
					// ignore
				}
			}
		}
	}
	public static String getOu(String sam) {
		// Given a samaccountname, return the OU it is in
		String retval = null;
		NamingEnumeration<SearchResult> results = null;
		SearchControls sc = null;
		DirContext dc = null;
		try {
			dc = LDAPConnectionFactory.getAdminConnection();
			if (dc == null || sam == null || sam.equals("")) {
				return null;
			}
			sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			
			PCConfig config = PCConfig.getInstance();
			
			String searchBase = config.getProperty("ldap.searchbase", true);
			
			results = dc.search(searchBase,  "samaccountname="+sam,sc);
			if (results == null || ! results.hasMore()) {
				return null;
			} else {
				String dn = results.next().getNameInNamespace();
				dn = dn.replaceAll("^[^,]*,", "");  // Remove everything before the first comma
				retval = dn;
			}
			return retval;
		} catch (Exception e) {
			return retval;
		} finally {
			if (results != null) {
				try {
					results.close();
				} catch (Exception ign) {
					//ignore
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
	}

	public static ADUser loadAdUser(String sam) {
		// Given a sAMAccountName, find the user and load up 
		// an ADUser object with it.
		ADUser retval = new ADUser();
		retval.setsAMAccountName(sam);
		retval.setAdPassword(null);
		PCConfig config = PCConfig.getInstance();
		retval.setAdDomain(config.getProperty("ldap.domain", true));
		retval.setAdOu(LDAPUtils.getOu(sam));
		retval.setMemberships(LDAPUtils.getMemberships(sam));
		retval.setCreated(true); // created already if we found it
		return retval;
	}
}
