package edu.duke.oit.idms.proconsul.util;

import java.util.HashMap;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.sun.istack.internal.logging.Logger;

import edu.duke.oit.idms.proconsul.cfg.PCConfig;

public class LDAPAdminConnection extends LDAPConnection {

	/**
	 * ldap connection using admin principal and connection pooling
	 * @author rob
	 */
	private static final Logger LOG = Logger.getLogger(LDAPAdminConnection.class);
	private static LDAPAdminConnection conn = null;
	private static HashMap<String,LDAPAdminConnection> connMap = new HashMap<String,LDAPAdminConnection>();
	private Hashtable<String,String> environment = new Hashtable<String,String>();
	
	private LDAPAdminConnection(String target) {
		PCConfig config = PCConfig.getInstance();
		System.setProperty("com.sun.jndi.ldap.connect.pool.protocol", "plain ssl");
		System.setProperty("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
		environment.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
		environment.put("java.naming.ldap.version", "3");
		environment.put("com.sun.jndi.ldap.connect.pool", "true");
		environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		environment.put(Context.SECURITY_AUTHENTICATION, "simple");
		environment.put(Context.PROVIDER_URL, target);
		environment.put(Context.SECURITY_PRINCIPAL, config.getProperty("ldap.binddn", true));
		environment.put(Context.SECURITY_CREDENTIALS, config.getProperty("ldap.password", true));
		environment.put(Context.REFERRAL, "follow");
		
		// Ignore ssl certificate trust anchor requirements with override SSL library
		
		//environment.put("java.naming.ldap.factory.socket",  edu.duke.oit.idms.oracle.ssl.BlindSSLSocketFactory.class.getName());
		environment.put("java.naming.ldap.factory.socket",  PoolableBlindSSLSocketFactory.class.getName());
	}
	private LDAPAdminConnection() {
		this(PCConfig.getInstance().getProperty("ldap.provider", true));
	}
	private void addMap(LDAPAdminConnection c) {
		String url = environment.get(Context.PROVIDER_URL);
		LOG.info("Adding cached connection to " + url);
		if (! connMap.containsKey(url)) {
			connMap.put(url,c);
		}
	}
	protected static LDAPAdminConnection getInstance() {
		if (conn == null) {
			conn = new LDAPAdminConnection();
			conn.addMap(conn);
		}
		for (String key : connMap.keySet()) {
			LOG.info("ConnMap contains: " + key);
		}
		return conn;
	}
	protected static LDAPAdminConnection getInstance(String target) {
		/*if (!connMap.containsKey(target)) {
			LOG.info("New admin connection with target " + target);
			conn.addMap(new LDAPAdminConnection(target));
		}
		LOG.info("Returning admin connection with target " + target);
		return connMap.get(target);*/
		if (conn == null || !connMap.containsKey(target)) {
			conn = new LDAPAdminConnection(target);
			conn.addMap(conn);
		} else {
			conn = connMap.get(target);
		}
		for (String key : connMap.keySet()) {
			LOG.info("connMap contains: " + key);
		}
		return conn;
	}
	@Override
	public DirContext getConnection() throws NamingException {
		return new InitialDirContext(environment);
	}
}
