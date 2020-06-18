package edu.duke.oit.idms.proconsul;

import java.util.ArrayList;
import java.util.Date;

import edu.duke.oit.idms.proconsul.util.ADUser;
import edu.duke.oit.idms.proconsul.util.AuthUser;

public class CheckedOutCred {

	private AuthUser authUser;
	private ADUser targetUser;
	private String targetHost;
	private String reason;
	private Date startTime;
	private ArrayList<String> groups;
	private String status;
	private int lifetime;
	private long expirationTime;
	private String expirationDate;
	
	
	public String getExpirationDate() {
		return expirationDate;
	}
	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}
	public AuthUser getAuthUser() {
		return authUser;
	}
	public void setAuthUser(AuthUser authUser) {
		this.authUser = authUser;
	}
	public ADUser getTargetUser() {
		return targetUser;
	}
	public void setTargetUser(ADUser targetUser) {
		this.targetUser = targetUser;
	}
	public String getTargetHost() {
		return targetHost;
	}
	public void setTargetHost(String targetHost) {
		this.targetHost = targetHost;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public ArrayList<String> getGroups() {
		return groups;
	}
	public void setGroups(ArrayList<String> groups) {
		this.groups = groups;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getLifetime() {
		return lifetime;
	}
	public void setLifetime(int lifetime) {
		this.lifetime = lifetime;
	}
	public long getExpirationTime() {
		return expirationTime;
	}
	public void setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
	}
	
	
}
