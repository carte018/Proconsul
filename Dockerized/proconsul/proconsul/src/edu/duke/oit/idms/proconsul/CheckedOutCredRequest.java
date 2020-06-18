package edu.duke.oit.idms.proconsul;

import java.util.ArrayList;

public class CheckedOutCredRequest {

	// POJO to hold parameters for making a request for a checked-out credential
	
	private String targetHost;
	private int lifetime;  // in hours
	private long expiration;  // epochal milliseconds
	private String reason;
	private String csrfToken;
	private ArrayList<String> groups;
	
	
	public ArrayList<String> getGroups() {
		return groups;
	}
	public void setGroups(ArrayList<String> groups) {
		this.groups = groups;
	}
	public String getCsrfToken() {
		return csrfToken;
	}
	public void setCsrfToken(String csrfToken) {
		this.csrfToken = csrfToken;
	}
	public String getTargetHost() {
		return targetHost;
	}
	public void setTargetHost(String targetHost) {
		this.targetHost = targetHost;
	}
	public int getLifetime() {
		return lifetime;
	}
	public void setLifetime(int lifetime) {
		this.lifetime = lifetime;
	}
	public long getExpiration() {
		return expiration;
	}
	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	
}
