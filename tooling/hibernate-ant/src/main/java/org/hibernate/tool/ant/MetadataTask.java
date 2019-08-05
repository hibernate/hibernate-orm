package org.hibernate.tool.ant;

import java.io.File;

public class MetadataTask {
	
	File propertyFile = null;
	File configFile = null;
	
	public void setPropertyFile(File file) {
		this.propertyFile = file;
	}
	
	public void setConfigFile(File file) {
		this.configFile = file;
	}
	
}
