package edu.duke.oit.idms.proconsul;

import java.util.Date;

import edu.duke.oit.idms.proconsul.util.ADUser;
import edu.duke.oit.idms.proconsul.util.AuthUser;

public class Session {

	private String targetSystem;
	private AuthUser authUser;
	private ADUser targetUser;
	private Status status;
	private int vncPort;
	private String vncPassword;
	private String displayName;
	private Date startTime;
	private String sessionID;
	private String type;
	private String delegatedou;
	private String delegatedrole;
	private String gatewayfqdn;
	
	public String getGatewayfqdn() {
		return gatewayfqdn;
	}
	public void setGatewayfqdn(String gatewayfqdn) {
		this.gatewayfqdn = gatewayfqdn;
	}
	public String getTargetSystem() {
		return targetSystem;
	}
	public void setTargetSystem(String targetSystem) {
		this.targetSystem = targetSystem;
	}
	public AuthUser getAuthUser() {
		return authUser;
	}
	public void setAuthUser(AuthUser au) {
		this.authUser = au;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public int getVncPort() {
		return vncPort;
	}
	public void setVncPort(int vncPort) {
		this.vncPort = vncPort;
	}
	public String getVncPassword() {
		return vncPassword;
	}
	public void setVncPassword(String vncPassword) {
		this.vncPassword = vncPassword;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public String getSessionID() {
		return sessionID;
	}
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
	public ADUser getTargetUser() {
		return targetUser;
	}
	public void setTargetUser(ADUser targetUser) {
		this.targetUser = targetUser;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getDelegatedou() {
		return delegatedou;
	}
	public void setDelegatedou(String delegatedou) {
		this.delegatedou = delegatedou;
	}
	public String getDelegatedrole() {
		return delegatedrole;
	}
	public void setDelegatedrole(String delegatedrole) {
		this.delegatedrole = delegatedrole;
	}
}
