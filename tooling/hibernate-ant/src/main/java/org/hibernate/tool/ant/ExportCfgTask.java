package org.hibernate.tool.ant;

import java.io.File;

public class ExportCfgTask {
	
	boolean executed = false;
	HibernateToolTask parent = null;
	File destinationFolder = null;
	
	public ExportCfgTask(HibernateToolTask parent) {
		this.parent = parent;
	}
	
	public void setDestinationFolder(File destinationFolder) {
		this.destinationFolder = destinationFolder;
	}
	
	public File getDestinationFolder() {
		return this.destinationFolder;
	}
	
	public void execute() {
		executed = true;
	}

}
