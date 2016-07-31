package edu.duke.oit.idms.proconsul;

public class DomainAdminRequest {
	// POJO to represent the data returned by the form requesting a domain admin session
	//
	String targetFQDN;
	boolean exposePassword;
	String displayName;
	String csrfToken;
	public String getCsrfToken() {
		return csrfToken;
	}
	public void setCsrfToken(String csrfToken) {
		this.csrfToken = csrfToken;
	}
	public String getTargetFQDN() {
		return targetFQDN;
	}
	public void setTargetFQDN(String targetFQDN) {
		this.targetFQDN = targetFQDN;
	}
	public boolean isExposePassword() {
		return exposePassword;
	}
	public void setExposePassword(boolean exposePassword) {
		this.exposePassword = exposePassword;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
}
