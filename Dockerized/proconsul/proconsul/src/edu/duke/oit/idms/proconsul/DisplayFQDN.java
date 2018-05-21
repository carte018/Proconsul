package edu.duke.oit.idms.proconsul;

public class DisplayFQDN {
	// POJO to encapsulate information for displaying FQDNs in UI
	//
	private String fqdn;
	private String displayname;
	private String delegatedOU;
	private String delegatedRole;
	
	public DisplayFQDN(String fqdn, String displayname) {
		this.fqdn = fqdn;
		if (displayname == null) {
			this.displayname = fqdn;
		} else {
			try {
				this.displayname = displayname + " (" + fqdn.substring(0,fqdn.indexOf('.')) + ")";
			} catch (Exception e) {
				this.displayname = displayname;
			}
		}
	}

	public String getFqdn() {
		return fqdn;
	}

	public void setFqdn(String fqdn) {
		this.fqdn = fqdn;
	}

	public String getDisplayname() {
		return displayname;
	}

	public void setDisplayname(String displayname) {
		this.displayname = displayname;
	}

	public String getDelegatedOU() {
		return delegatedOU;
	}

	public void setDelegatedOU(String delegatedOU) {
		this.delegatedOU = delegatedOU;
	}

	public String getDelegatedRole() {
		return delegatedRole;
	}

	public void setDelegatedRole(String delegatedRole) {
		this.delegatedRole = delegatedRole;
	}
}
