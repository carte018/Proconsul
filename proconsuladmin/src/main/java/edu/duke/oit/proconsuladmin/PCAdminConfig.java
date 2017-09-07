package edu.duke.oit.proconsuladmin;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;


public class PCAdminConfig {

	private static PCAdminConfig config = null;
	private Properties properties = null;
	
	private PCAdminConfig() {
		properties = new Properties();
		
		ClassLoader cl = PCAdminConfig.class.getClassLoader();
		URL url = cl.getResource("proconsuladmin.properties");
		
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
	public static PCAdminConfig getInstance() {
		if (config == null) {
			config = new PCAdminConfig();
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
