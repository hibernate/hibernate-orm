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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.tools.ant.types.Environment.Variable;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tools.test.util.HibernateUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ExportCfgTaskTest {
	
	@TempDir 
	Path tempdir;
	
	@Test 
	public void testExportCfgTask() {
		HibernateToolTask htt = new HibernateToolTask();
		ExportCfgTask ect = new ExportCfgTask(htt);
		assertSame(htt, ect.parent);
	}
	
	@Test
	public void testExecute() throws Exception {
		ExportCfgTask ect = new ExportCfgTask(null);
		Properties properties = new Properties();
		properties.put(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
		properties.put(AvailableSettings.CONNECTION_PROVIDER, HibernateUtil.ConnectionProvider.class.getName());
		MetadataDescriptor mdd = MetadataDescriptorFactory.createNativeDescriptor(
				null, 
				new File[] {}, 
				properties);
		ect.properties.put(ExporterConstants.METADATA_DESCRIPTOR, mdd);
		File destinationFolder = tempdir.toFile();
		ect.properties.put(ExporterConstants.DESTINATION_FOLDER, destinationFolder);
		File cfgFile = new File(destinationFolder, "hibernate.cfg.xml");
		assertFalse(cfgFile.exists());
		ect.execute();
		assertTrue(cfgFile.exists());
		String cfgXmlString = new String(Files.readAllBytes(cfgFile.toPath()));
		assertTrue(cfgXmlString.contains("hibernate.dialect"));
	}
	
	@Test
	public void testSetDestinationFolder() {
		ExportCfgTask ect = new ExportCfgTask(null);
		assertNull(ect.properties.get(ExporterConstants.DESTINATION_FOLDER));
		File file = new File("/");
		ect.setDestinationFolder(file);
		assertSame(file, ect.properties.get(ExporterConstants.DESTINATION_FOLDER));
	}
	
	@Test
	public void testAddConfiguredProperty() {
		ExportCfgTask ect = new ExportCfgTask(null);
		assertNull(ect.properties.get("foo"));
		Variable v = new Variable();
		v.setKey("foo");
		v.setValue("bar");
		ect.addConfiguredProperty(v);
		assertEquals("bar", ect.properties.get("foo"));
	}
	
	@Test
	public void testSetMetadataDescriptor() {
		ExportCfgTask ect = new ExportCfgTask(null);
		MetadataDescriptor mdd = MetadataDescriptorFactory.createNativeDescriptor(null, null, null);
		assertNull(ect.properties.get(ExporterConstants.METADATA_DESCRIPTOR));
		ect.setMetadataDescriptor(mdd);
		assertSame(mdd, ect.properties.get(ExporterConstants.METADATA_DESCRIPTOR));
	}
	
	@Test
	public void testSetTemplatePath() {
		ExportCfgTask ect = new ExportCfgTask(null);
		assertNull(ect.properties.get(ExporterConstants.TEMPLATE_PATH));
		ect.setTemplatePath(tempdir);
		assertSame(tempdir, ect.properties.get(ExporterConstants.TEMPLATE_PATH));
	}
	
	@Test
	public void testGetProperties() {
		ExportCfgTask ect = new ExportCfgTask(null);
		assertSame(ect.properties, ect.getProperties());
	}

}
