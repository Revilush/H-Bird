package search;


import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class PropertiesManager {
	
	private static final Logger logger = Logger.getLogger(PropertiesManager.class.getName());

	public static final String COMMON_PROPERTIES_FILE = "config.properties";

	// Works as cache
	private Properties commonProperties = null;
	
	private static PropertiesManager propManager = new PropertiesManager();

	private PropertiesManager() {
		try {
			commonProperties = new Properties();
			commonProperties.load(this.getClass().getClassLoader().getResourceAsStream(COMMON_PROPERTIES_FILE));
		} catch (IOException ex) {
			logger.fatal("Exception while creating PropertiesManager: ", ex);
		}
	}
	
	/**
	 * Returns an instance of the PropertiesManager.
	 * @return PropertiesManager
	 */
	public static PropertiesManager getInstance() {
		return propManager;
	}
	
	/**
	 * Returns properties common to portal, defined in 'common.properties'.
	 * @param propKey
	 * @return
	 */
	public String getCommonProperty(String propKey) {
		if (commonProperties != null)
			return commonProperties.getProperty(propKey);	
		return null;
	}
	public String getCommonProperty(String propKey, String defaultValue) {
		String propVal = getCommonProperty(propKey);
		if (propVal != null)
			return propVal;
		return defaultValue; 
	}

	
	public String getDeveloperAccountId() {
		return getCommonProperty("amazon.developer.account.id", "9249-3921-4118*");
	}
	public String getSecretKey() {
		return getCommonProperty("amazon.secret.key", "c8cksLDzjpvz/hVGbVbFnIsGcJUlpM9xFRZ6UlhS");
	}
	public String getApplicationName() {
		return getCommonProperty("application.name", "Kaytrader's Aggregator");
	}
	public String getApplicationVersion() {
		return getCommonProperty("application.version", "1.0");
	}
	
	
	public synchronized void reLoad() {
		try {
			//keep a ref to existing properties
			Properties oldProps = commonProperties;
			//initialize a new one
			Properties newProps = new Properties();
			newProps.load(this.getClass().getClassLoader().getResourceAsStream(COMMON_PROPERTIES_FILE));
			//use the new one
			commonProperties = newProps;
			//clear the old one
			oldProps.clear();
		} catch(IOException ex) {
			logger.fatal("Exception while reloading properties in PropertiesManager: ", ex);
		}
	}

}
