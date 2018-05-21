package edu.duke.oit.idms.proconsul.util;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

public abstract class LDAPConnection {

	/**
	 * @return DirContext
	 * @throws NamingException
	 * 
	 */
	public abstract DirContext getConnection() throws NamingException;
	
}
