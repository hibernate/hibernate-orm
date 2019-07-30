package org.hibernate.tool.ant;

public class ExportCfgTask {
	
	boolean executed = false;
	HibernateToolTask parent = null;
	
	public ExportCfgTask(HibernateToolTask parent) {
		this.parent = parent;
	}
	
	public void execute() {
		executed = true;
	}

}
