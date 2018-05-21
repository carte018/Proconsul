package edu.duke.oit.idms.proconsul.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;

import edu.duke.oit.idms.proconsul.Status;
import edu.duke.oit.idms.proconsul.cfg.PCConfig;

public class ProconsulSession implements Comparable<ProconsulSession> {

	// Represent a proconsul session to the back end system
	//
	// Properties
	private AuthUser owner;
	private String fqdn;
	private int novncPort;
	private String vncPassword;
	private Status status;
	private ADUser targetUser;
	private Date startTime;
	private String displayName;
	private String type;
	private String delegatedou;
	private String delegatedrole;
	private String gatewayfqdn;
	
	// Constructors
	
	public String getGatewayfqdn() {
		return gatewayfqdn;
	}
	public void setGatewayfqdn(String gatewayfqdn) {
		this.gatewayfqdn = gatewayfqdn;
	}
	public ProconsulSession() {
		status = Status.STARTING;
		targetUser = new ADUser();
		owner = new AuthUser();
		type = "user";  // default session type is "user"
			}
	public ProconsulSession(String tval) {
		status = Status.STARTING;
		targetUser = new ADUser();
		owner = new AuthUser();
		type = tval;
	}
	public ProconsulSession(AuthUser au, String host, String vp, ADUser tu, String dname) {
		status = Status.STARTING;
		owner = 	au;
		fqdn = host;
		vncPassword = vp;
		targetUser = tu;
		startTime = new Date();
		displayName = dname;
		type = "user";  // default type is "user"
	}
	public ProconsulSession(AuthUser au, String host, String vp, ADUser tu, String dname, String tval) {
		status = Status.STARTING;
		owner = au;
		fqdn = host;
		vncPassword = vp;
		targetUser = tu;
		startTime = new Date();
		displayName = dname;
		type = tval;
	}
	public ProconsulSession(AuthUser au, String host, String vp, ADUser tu, String dname, String tval, String delou, String delrole) {
		status = Status.STARTING;
		owner = au;
		fqdn = host;
		vncPassword = vp;
		targetUser = tu;
		startTime = new Date();
		displayName = dname;
		type = tval;
		if (tval.equalsIgnoreCase("delegated")) {
			delegatedou = delou;
			delegatedrole = delrole;
		}
	}
	public AuthUser getOwner() {
		return owner;
	}
	public void setOwner(AuthUser owner) {
		this.owner = owner;
	}
	public String getFqdn() {
		return fqdn;
	}
	public void setFqdn(String fqdn) {
		this.fqdn = fqdn;
	}
	public int getNovncPort() {
		return novncPort;
	}
	public void setNovncPort(int novncPort) {
		this.novncPort = novncPort;
	}
	public String getVncPassword() {
		return vncPassword;
	}
	public void setVncPassword(String vncPassword) {
		this.vncPassword = vncPassword;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public ADUser getTargetUser() {
		return targetUser;
	}
	public void setTargetUser(ADUser targetUser) {
		this.targetUser = targetUser;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	// utilities
	
	public boolean isConnected() {
		switch(status) {
		case CONNECTED:
			return true;
		default:
			return false;
		}
	}
	public boolean isAvailable() {
		switch(status) {
		case STARTING:
		case TERMINATING:
			return false;
		default:
			return true;
			
		}
	}
	public void randomizeVncpassword(int length) {
		vncPassword = RandomStringUtils.randomAlphanumeric(length);
	}
	public int compareTo(ProconsulSession x) {
		return (fqdn.compareTo(x.getFqdn()));
	}
	public String printableStartTime() {
		String fmt = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(fmt);
		return (String) sdf.format(this.getStartTime());
	}
	public String getRedirectUrl() {
		// Construct a URL for redirecting to this session
		//
		String retval = null;
		PCConfig config = PCConfig.getInstance();
		retval = "https://" + config.getProperty("novnc.hostname",true) + config.getProperty("novnc.uri", true) + "?encrypt=1&autoconnect=1&port=" + String.valueOf(5901 + this.getNovncPort()) + "&password=" + this.getVncPassword();
		return retval;
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

