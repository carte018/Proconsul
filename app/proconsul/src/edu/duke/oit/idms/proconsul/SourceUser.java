package edu.duke.oit.idms.proconsul;

import java.util.ArrayList;

public class SourceUser {
	private String displayName;
	private String eppn;
	private ArrayList<String> memberships;
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getEppn() {
		return eppn;
	}
	public void setEppn(String eppn) {
		this.eppn = eppn;
	}
	public ArrayList<String> getMemberships() {
		return memberships;
	}
	public void setMemberships(ArrayList<String> memberships) {
		this.memberships = memberships;
	}
}
