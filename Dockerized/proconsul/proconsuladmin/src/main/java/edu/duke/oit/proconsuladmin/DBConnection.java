package edu.duke.oit.proconsuladmin;

import java.sql.Connection;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public abstract class DBConnection {

	private ComboPooledDataSource cpds;
	
	// In case database changes around, parameterize it
	
	protected DBConnection(String driver, String url, String user, String password) {
		cpds = new ComboPooledDataSource();
		try {
			cpds.setDriverClass(driver);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		cpds.setJdbcUrl(url);
		cpds.setUser(user);
		cpds.setPassword(password);
		
		// Management rules
		
		cpds.setIdleConnectionTestPeriod(300);
		cpds.setMaxIdleTime(3600);
		cpds.setMinPoolSize(4);
		cpds.setPreferredTestQuery("SELECT 1 FROM DUAL");
	}
	protected Connection getConnection() throws SQLException {
		return cpds.getConnection();
	}
	protected void finalize() throws Throwable {
		if (cpds != null) {
			cpds.close();
		}
		super.finalize();
	}
}
