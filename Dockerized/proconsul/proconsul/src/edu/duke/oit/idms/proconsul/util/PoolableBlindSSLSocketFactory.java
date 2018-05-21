package edu.duke.oit.idms.proconsul.util;

import java.util.Comparator;

import javax.net.SocketFactory;

//import com.sun.istack.internal.logging.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.duke.oit.idms.oracle.ssl.BlindSSLSocketFactory;

public class PoolableBlindSSLSocketFactory extends BlindSSLSocketFactory
		implements java.util.Comparator<SocketFactory> {

	private static Logger LOG = LoggerFactory.getLogger(PoolableBlindSSLSocketFactory.class);
	
	@Override
	public int compare(SocketFactory o1, SocketFactory o2) {
		LOG.info("Called compare on socketfactory in blind socket override");
		if (o1.getClass().getName().equalsIgnoreCase(o2.getClass().getName())) {
			return 0;
		} else {
			return 1;
		}
	}
	
	public int compare(String o1, String o2) {
		// TODO Auto-generated method stub
		LOG.info("Called compare in blind socket override");
		if (o1.equalsIgnoreCase(o2)) {
			return 0;
		} else {
			return 1;
		}
	}
	
	public boolean equals(String o1, String o2) {
		return (o1.equalsIgnoreCase(o2));
	}
}
