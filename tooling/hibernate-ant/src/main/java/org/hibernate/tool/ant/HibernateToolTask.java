package org.hibernate.tool.ant;

public class HibernateToolTask {
	
	boolean hasConfiguration = false;
	
	public ConfigurationTask createConfiguration() {
		return new ConfigurationTask();
	}
	
	public void execute() {
		// do nothing for now
	}
	
}
