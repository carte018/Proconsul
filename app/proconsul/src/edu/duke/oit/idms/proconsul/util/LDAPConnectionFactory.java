package edu.duke.oit.idms.proconsul.util;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

public class LDAPConnectionFactory {
	
	public static DirContext getAdminConnection(String target) throws NamingException {
		return LDAPAdminConnection.getInstance(target).getConnection();
	}

	public static DirContext getAdminConnection() throws NamingException {
		return LDAPAdminConnection.getInstance().getConnection();
	}
}
	