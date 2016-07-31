package edu.duke.oit.idms.proconsul;

public class ReconnectRequest {
	// Bean for backing the reconnect form in the main page of Proconsul
	//
	private String targetFQDN;
	private String requestType;  // either "reconnect" or "terminate"
	private String csrfToken;
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
	public String getRequestType() {
		return requestType;
	}
	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}
}
