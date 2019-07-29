package org.hibernate.tool.ant;

public class HibernateToolTask {
	
	public MetadataTask createMetadata() {
		return new MetadataTask();
	}
	
	public ExportCfgTask createExportCfg() {
		return new ExportCfgTask();
	}
	
	public ExportDdlTask createExportDdl() {
		return new ExportDdlTask();
	}
	
	public void execute() {
		// do nothing for now
	}
	
}
