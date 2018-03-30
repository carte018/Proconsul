package edu.duke.oit.idms.proconsul;

public class StaticUserSessionRequest {
	// POJO to hold references from a User Session request
	//
	// Just like a UserSessionRequest, but used for static sessions
	// May eventually vary depending on requirements for static session handling
	//
	
	String targetFQDN;
	String displayName;
	String csrfToken;
	String resolution;
	public String getTargetFQDN() {
		return targetFQDN;
	}
	public void setTargetFQDN(String targetFQDN) {
		this.targetFQDN = targetFQDN;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getCsrfToken() {
		return csrfToken;
	}
	public void setCsrfToken(String csrfToken) {
		this.csrfToken = csrfToken;
	}
	public String getResolution() {
		return resolution;
	}
	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

}
