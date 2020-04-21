/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 * 
 * Copyright 2004-2020 Red Hat, Inc.
 *
 * Licensed under the GNU Lesser General Public License (LGPL), 
 * version 2.1 or later (the "License").
 * You may not use this file except in compliance with the License.
 * You may read the licence in the 'lgpl.txt' file in the root folder of 
 * project or obtain a copy at
 *
 *     http://www.gnu.org/licenses/lgpl-2.1.html
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
