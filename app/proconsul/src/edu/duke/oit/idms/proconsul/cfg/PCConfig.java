package edu.duke.oit.idms.proconsul.cfg;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;


public class PCConfig {

	private static PCConfig config = null;
	private Properties properties = null;
	
	private PCConfig() {
		properties = new Properties();
		
		ClassLoader cl = PCConfig.class.getClassLoader();
		URL url = cl.getResource("proconsul.properties");
		
		InputStream inputStream = null;
		try {
			inputStream = url.openStream();
			properties.load(inputStream);
		} catch (Exception e) {
			throw new RuntimeException("problem loading configuration");
		} finally { 
			if (inputStream != null) {
				try {
					inputStream.close();
				}catch (Exception e) {
					// ignore
				}
			}
		}
	}
	/**
	 *
	 * @return config
	 */
	public static PCConfig getInstance() {
		if (config == null) {
			config = new PCConfig();
		}
		return config;
	}
	
	/**
	 * get property
	 * @param property
	 * @param exceptionIfNotFound
	 * @return property value
	 */
	public String getProperty(String property, boolean exceptionIfNotFound) {
		String value = properties.getProperty(property);
		if (value == null && exceptionIfNotFound) {
			throw new RuntimeException("property: " + property + " not found");
		}
		return value;
	}
		
}
