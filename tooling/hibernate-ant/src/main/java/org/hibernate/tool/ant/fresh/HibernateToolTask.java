package org.hibernate.tool.ant.fresh;

import org.hibernate.tool.api.export.ExporterConstants;

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
		if (exportCfgTask != null) {
			exportCfgTask.getProperties().put(
					ExporterConstants.METADATA_DESCRIPTOR, 
					metadataTask.createMetadataDescriptor());
			exportCfgTask.execute();
		}
	}
	
}
