package org.hibernate.tool.ant;

import java.io.File;
import java.util.Properties;

import org.apache.tools.ant.types.Environment.Variable;
import org.hibernate.tool.api.export.ExporterConstants;

public class ExportCfgTask {
	
	boolean executed = false;
	HibernateToolTask parent = null;
	Properties properties = new Properties();
	
	public ExportCfgTask(HibernateToolTask parent) {
		this.parent = parent;
	}
	
	public void setDestinationFolder(File destinationFolder) {
		this.properties.put(ExporterConstants.OUTPUT_FOLDER, destinationFolder);
	}
	
	public File getDestinationFolder() {
		return (File)this.properties.get(ExporterConstants.OUTPUT_FOLDER);
	}
	
	public void addConfiguredProperty(Variable variable) {
		properties.put(variable.getKey(), variable.getValue());
	}
	
	public void execute() {
		executed = true;
	}

}
