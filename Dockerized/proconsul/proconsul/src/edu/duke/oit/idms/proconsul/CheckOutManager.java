package edu.duke.oit.idms.proconsul;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.duke.oit.idms.proconsul.cfg.PCConfig;
import edu.duke.oit.idms.proconsul.util.ADUser;
import edu.duke.oit.idms.proconsul.util.AuthUser;
import edu.duke.oit.idms.proconsul.util.DatabaseConnectionFactory;
import edu.duke.oit.idms.proconsul.util.ProconsulUtils;

public class CheckOutManager extends TimerTask {

	private static CheckOutManager com = null;
	private static Object mutex = new Object();

	
	private static final Logger LOG = LoggerFactory.getLogger(CheckOutManager.class);
	static long runcount = 0;
	static long lastrunduration = 0;
	static long lastrunstart = 0;
	static long maxduration = 0;
	static String status = "Idle";
	public static long getRuncount() {
		return runcount;
	}
	public static void setRuncount(long runcount) {
		CheckOutManager.runcount = runcount;
	}
	public static long getLastrunduration() {
		return lastrunduration;
	}
	public static void setLastrunduration(long lastrunduration) {
		CheckOutManager.lastrunduration = lastrunduration;
	}
	public static long getLastrunstart() {
		return lastrunstart;
	}
	public static void setLastrunstart(long lastrunstart) {
		CheckOutManager.lastrunstart = lastrunstart;
	}
	public static long getMaxduration() {
		return maxduration;
	}
	public static void setMaxduration(long maxduration) {
		CheckOutManager.maxduration = maxduration;
	}
	public static String getStatus() {
		return status;
	}
	public static void setStatus(String status) {
		CheckOutManager.status = status;
	}
	
	private CheckOutManager() {
		
	}
	
	public static CheckOutManager getInstance() {
		if (com == null) {
			synchronized (mutex) {
				com = new CheckOutManager();
			}
		}
		return com;
	}
	
	@Override
	public void run() {
		// Our goal is to periodically scan the checkout table in the database looking for 
		// expired but undeleted AD users, deleting them, and then updating the database
		// to reflect (a) that the checkout is inactive and (b) that the AD DA entry has 
		// expired (since we prophyllactically add DA entries for every checkout we perform.
		//
		// Simplistically, we just run through the entries in the DB table every time we
		// awaken.
		//

		PCConfig config = PCConfig.getInstance();

		Connection pcdb = null;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		if (status.equals("active")) {
			return;  // don't start a second instance of ourselves;
		}
		status = "active";
		try {
			ProconsulUtils.debug(LOG,"Running checkout manager");
			pcdb = DatabaseConnectionFactory.getProconsulDatabaseConnection();
			if (pcdb != null) {
				ps = pcdb.prepareStatement("select * from checkout where status = 'active' and expiretime <= ?");;
				if (ps != null) {
					ps.setLong(1, System.currentTimeMillis());
					rs = ps.executeQuery();
					while (rs != null && rs.next()) {
						// For each of the returned entries, do the following
						// Delete the relevant user object
						ADUser adu = new ADUser();
						adu.setAdDomain(config.getProperty("ldap.domain", true));
						adu.setAdOu(config.getProperty("ldap.targetbase", true));
						adu.setsAMAccountName(rs.getString("aduser"));
						AuthUser au = new AuthUser();
						au.setUid(rs.getString("eppn"));
						ProconsulUtils.deleteAdUser(adu,au,null);
						
						// Then update the status of the user in the DB
						ps2 = pcdb.prepareStatement("update checkout set status = 'inactive' where aduser = ?");
						if (ps2 != null) {
							ps2.setString(1, rs.getString("aduser"));
							ps2.executeUpdate();
						}
						
						// And update the status in the DA table
						ProconsulUtils.removeAuthorizedDA(adu,au,null);
					}
				} else {
					status = "inactive";
					return;
				}
			} else {
				status = "inactive";
				return;
			}
		} catch (Exception e) {
			status = "inactive";
			return;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception ign) {
					// ignore
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (rs2 != null) {
				try {
					rs2.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (ps2 != null) {
				try {
					ps2.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			if (pcdb != null) {
				try {
					pcdb.close();
				} catch (Exception ign) {
					//ignore
				}
			}
			status = "inactive";
		}

	}
}
