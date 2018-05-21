package edu.duke.oit.proconsuladmin;


public class PCAdminDBConnection extends DBConnection {
	
	private static PCAdminDBConnection instance = null;
	
	protected PCAdminDBConnection() {
		super (PCAdminConfig.getInstance().getProperty("pcdb.driver", true),
				PCAdminConfig.getInstance().getProperty("pcdb.url", true),
				PCAdminConfig.getInstance().getProperty("pcdb.user", true),
				PCAdminConfig.getInstance().getProperty("pcdb.password", true));
		
	}
	protected static PCAdminDBConnection getInstance() {
		if (instance == null) {
			instance = new PCAdminDBConnection();
			
		} 
		return instance;
	}
}
