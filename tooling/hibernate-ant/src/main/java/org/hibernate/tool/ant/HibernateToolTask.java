package org.hibernate.tool.ant;

public class HibernateToolTask {
	
	public ConfigurationTask createConfiguration() {
		return new ConfigurationTask();
	}
	
	public MetadataTask createMetadata() {
		return new MetadataTask();
	}
	
	public void execute() {
		// do nothing for now
	}
	
}
