package org.hibernate.tool.ant;

public class HibernateToolTask {
	
	MetadataTask metadataTask;
	
	public MetadataTask createMetadata() {
		this.metadataTask = new MetadataTask();
		return this.metadataTask;
	}
	
	public ExportCfgTask createExportCfg() {
		return new ExportCfgTask(this);
	}
	
	public ExportDdlTask createExportDdl() {
		return new ExportDdlTask();
	}
	
	public void execute() {
		// do nothing for now
	}
	
}
