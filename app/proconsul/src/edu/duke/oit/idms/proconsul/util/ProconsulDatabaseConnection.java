package edu.duke.oit.idms.proconsul.util;

import edu.duke.oit.idms.proconsul.cfg.PCConfig;

public class ProconsulDatabaseConnection extends DatabaseConnection {
	
	private static ProconsulDatabaseConnection instance = null;
	
	protected ProconsulDatabaseConnection() {
		super (PCConfig.getInstance().getProperty("pcdb.driver", true),
				PCConfig.getInstance().getProperty("pcdb.url", true),
				PCConfig.getInstance().getProperty("pcdb.user", true),
				PCConfig.getInstance().getProperty("pcdb.password", true));
		
	}
	protected static ProconsulDatabaseConnection getInstance() {
		if (instance == null) {
			instance = new ProconsulDatabaseConnection();
			
		} 
		return instance;
	}
}
