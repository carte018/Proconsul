package edu.duke.oit.idms.proconsul.util;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

public class AuthUser {

	private String uid;
	private String name;
	private ArrayList<String> memberships;
	private ArrayList<String> entitlements;
	
	//Constructor
	public AuthUser() {
		memberships = new ArrayList<String>();
		entitlements = new ArrayList<String>();
	}
	public AuthUser(HttpServletRequest req) {
		// Initialize from an existing HTTP request
		memberships = new ArrayList<String>();
		entitlements = new ArrayList<String>();
		uid = req.getRemoteUser();
		String m = (String) req.getAttribute("isMemberOf");
		String[] ms = null;
		if (m != null) {
			ms = (m.split(";"));
			for (int i = 0; i < ms.length; i++) {
				if (ms[i] != null) {
					addMembership(ms[i]);
				}
			}
		}
		String e = (String) req.getAttribute("entitlements");
		String[] es = null;
		if (e != null) {
			es = e.split(";");
			for (int i = 0; i < es.length; i++) {
				if (es[i] != null) {
					addEntitlement(es[i]);
				}
			}
		}
	}
	private void addEntitlement(String string) {
		// TODO Auto-generated method stub
		entitlements.add(string);
	}
	private void addMembership(String string) {
		memberships.add(string);
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ArrayList<String> getMemberships() {
		return memberships;
	}
	public void setMemberships(ArrayList<String> memberships) {
		this.memberships = memberships;
	}
	public ArrayList<String> getEntitlements() {
		return entitlements;
	}
	public void setEntitlements(ArrayList<String> entitlements) {
		this.entitlements = entitlements;
	}
	
	// Utilities
	
	public boolean isMemberOf(String groupName) {
		// True if the membership list contains groupName
		if (memberships.contains(groupName)) {
			return true;
		} else {
			return false;
		}
	}
	public boolean hasEntitlement(String entitlement) {
		// True if the entitlement list contains entitlement
		if (entitlements.contains(entitlement)) {
			return true;
		} else {
			return false;
		}
	}
}
