package org.hibernate.tool.ant;

public class HibernateToolTask {
	
	public MetadataTask createMetadata() {
		return new MetadataTask();
	}
	
	public void execute() {
		// do nothing for now
	}
	
}
