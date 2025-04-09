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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.junit.jupiter.api.Test;

public class HibernateToolTaskTest {	
	
	@Test
	public void testCreateMetadata() {
		HibernateToolTask htt = new HibernateToolTask();
		assertNull(htt.metadataTask);
		MetadataTask mdt = htt.createMetadata();
		assertSame(mdt, htt.metadataTask);
	}

	@Test
	public void testCreateExportDdl() {
		HibernateToolTask htt = new HibernateToolTask();
		ExportDdlTask edt = htt.createExportDdl();
		assertNotNull(edt);
	}

	@Test
	public void testCreateExportCfg() {
		HibernateToolTask htt = new HibernateToolTask();
		assertNull(htt.exportCfgTask);
		ExportCfgTask ect = htt.createExportCfg();
		assertSame(ect, htt.exportCfgTask);
	}
	
	Object testObject = null;
	@Test
	public void testExecute() {
		HibernateToolTask htt = new HibernateToolTask();
		final MetadataDescriptor mdd = new MetadataDescriptor() {			
			@Override
			public Properties getProperties() {
				return null;
			}		
			@Override
			public Metadata createMetadata() {
				return null;
			}
		};
		MetadataTask mdt = new MetadataTask() {
			@Override
			public MetadataDescriptor createMetadataDescriptor() {
				return mdd;
			}
		};
		htt.metadataTask = mdt;
		ExportCfgTask ect = new ExportCfgTask(htt) {
			@Override 
			public void execute() {
				testObject = getProperties().get(ExporterConstants.METADATA_DESCRIPTOR);
			}
		};
		htt.exportCfgTask = ect;
		assertNull(testObject);
		htt.execute();
		assertSame(testObject, mdd);
	}

}
