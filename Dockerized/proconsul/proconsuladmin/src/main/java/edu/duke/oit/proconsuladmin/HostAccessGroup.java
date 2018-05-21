package edu.duke.oit.proconsuladmin;

public class HostAccessGroup {

	private String fqdn;
	private String ou;
	private String groupdn;
	public String getFqdn() {
		return fqdn;
	}
	public void setFqdn(String fqdn) {
		this.fqdn = fqdn;
	}
	public String getOu() {
		return ou;
	}
	public void setOu(String ou) {
		this.ou = ou;
	}
	public String getGroupdn() {
		return groupdn;
	}
	public void setGroupdn(String groupdn) {
		this.groupdn = groupdn;
	}
	
	
}
