package edu.duke.oit.proconsuladmin;

import java.sql.Connection;
import java.sql.SQLException;


public class DatabaseConnectionFactory {

	public static Connection getPCAdminDBConnection() throws SQLException {
		return PCAdminDBConnection.getInstance().getConnection();
		
	}
}
