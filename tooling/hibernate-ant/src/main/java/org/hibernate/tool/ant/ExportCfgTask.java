package org.hibernate.tool.ant;

import java.io.File;
import java.util.Properties;

import org.apache.tools.ant.types.Environment.Variable;

public class ExportCfgTask {
	
	boolean executed = false;
	HibernateToolTask parent = null;
	File destinationFolder = null;
	Properties properties = new Properties();
	
	public ExportCfgTask(HibernateToolTask parent) {
		this.parent = parent;
	}
	
	public void setDestinationFolder(File destinationFolder) {
		this.destinationFolder = destinationFolder;
	}
	
	public File getDestinationFolder() {
		return this.destinationFolder;
	}
	
	public void addConfiguredProperty(Variable variable) {
		properties.put(variable.getKey(), variable.getValue());
	}
	
	public void execute() {
		executed = true;
	}

}
