package edu.duke.oit.idms.proconsul.util;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionFactory {

	public static Connection getProconsulDatabaseConnection() throws SQLException {
		return ProconsulDatabaseConnection.getInstance().getConnection();
		
	}
}
