/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
