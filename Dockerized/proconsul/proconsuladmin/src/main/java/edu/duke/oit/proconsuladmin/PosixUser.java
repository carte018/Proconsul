package edu.duke.oit.proconsuladmin;

public class PosixUser {
	
	String eppn;
	int uidnumber;
	int gidnumber;
	String homedirectory;
	String loginshell;
	public String getEppn() {
		return eppn;
	}
	public void setEppn(String eppn) {
		this.eppn = eppn;
	}
	public int getUidnumber() {
		return uidnumber;
	}
	public void setUidnumber(int uidnumber) {
		this.uidnumber = uidnumber;
	}
	public int getGidnumber() {
		return gidnumber;
	}
	public void setGidnumber(int gidnumber) {
		this.gidnumber = gidnumber;
	}
	public String getHomedirectory() {
		return homedirectory;
	}
	public void setHomedirectory(String homedirectory) {
		this.homedirectory = homedirectory;
	}
	public String getLoginshell() {
		return loginshell;
	}
	public void setLoginshell(String loginshell) {
		this.loginshell = loginshell;
	}
	
}
