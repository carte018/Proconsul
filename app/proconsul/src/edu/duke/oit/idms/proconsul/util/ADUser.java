package edu.duke.oit.idms.proconsul.util;

import java.util.ArrayList;

// Class to encapsulate information about a dynamically-created
// user in the AD.
// User doens't actually convey session information.
// Sessions contain ADUser objects

public class ADUser {
	private String sAMAccountName;
	private String adPassword;
	private String adDomain;
	private String adOu;
	private ArrayList<String> memberships;
	private boolean created;
	
	public ADUser() {
		memberships = new ArrayList<String>();
		this.setCreated(false);
	}
	
	public String getsAMAccountName() {
		return sAMAccountName;
	}
	public void setsAMAccountName(String sAMAccountName) {
		this.sAMAccountName = sAMAccountName;
	}
	public String getAdPassword() {
		return adPassword;
	}
	public void setAdPassword(String adPassword) {
		this.adPassword = adPassword;
	}
	public String getAdDomain() {
		return adDomain;
	}
	public void setAdDomain(String adDomain) {
		this.adDomain = adDomain;
	}
	public String getAdOu() {
		return adOu;
	}
	public void setAdOu(String adOu) {
		this.adOu = adOu;
	}
	public ArrayList<String> getMemberships() {
		return memberships;
	}
	public void setMemberships(ArrayList<String> memberships) {
		this.memberships = memberships;
	}
	public boolean isCreated() {
		return created;
	}
	public void setCreated(boolean created) {
		this.created = created;
	}
}
