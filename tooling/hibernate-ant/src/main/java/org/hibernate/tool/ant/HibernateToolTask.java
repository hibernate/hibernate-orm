package org.hibernate.tool.ant;

public class HibernateToolTask {
	
	MetadataTask metadataTask;
	ExportCfgTask exportCfgTask;
	
	public MetadataTask createMetadata() {
		this.metadataTask = new MetadataTask();
		return this.metadataTask;
	}
	
	public ExportCfgTask createExportCfg() {
		this.exportCfgTask = new ExportCfgTask(this);
		return this.exportCfgTask;
	}
	
	public ExportDdlTask createExportDdl() {
		return new ExportDdlTask();
	}
	
	public void execute() {
		// do nothing for now
	}
	
}
