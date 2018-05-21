package edu.duke.oit.idms.proconsul.util;

public class ADConnectionsFactory {
	static ADConnections conns = null;
	
	public static ADConnections getInstance() {
		if (conns == null) {
			conns = new ADConnections();
		}
		return conns;
	}
}
