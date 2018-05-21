package edu.duke.oit.idms.proconsul;

import java.util.ArrayList;

public class OuHostList {
	// POJO to convey list of hosts for an OU in the AD 
	// Used in Ajax response code
	ArrayList<String> hosts;

	public ArrayList<String> getHosts() {
		return hosts;
	}

	public void setHosts(ArrayList<String> hosts) {
		this.hosts = hosts;
	}
}
