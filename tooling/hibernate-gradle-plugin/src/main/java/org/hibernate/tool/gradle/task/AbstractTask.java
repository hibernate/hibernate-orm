package org.hibernate.tool.gradle.task;

import java.util.Properties;

import org.gradle.api.DefaultTask;

public abstract class AbstractTask extends DefaultTask {

	private Properties properties = new Properties();
	
	void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}
	
	String getProperty(String key) {
		return properties.getProperty(key);
	}
	
}
