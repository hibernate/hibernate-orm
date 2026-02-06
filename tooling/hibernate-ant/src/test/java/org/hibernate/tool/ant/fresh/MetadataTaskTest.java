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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.hibernate.tool.ant.fresh.MetadataTask.Type;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

public class MetadataTaskTest {
	
	@TempDir
	Path tempDir;
	
	@Test
	public void testSetPersistenceUnit() {
		MetadataTask mdt = new MetadataTask();
		assertNull(mdt.persistenceUnit);
		mdt.setPersistenceUnit("foobar");
		assertEquals("foobar", mdt.persistenceUnit);
	}
	
	@Test
	public void testSetPropertyFile() {
		MetadataTask mdt = new MetadataTask();
		assertNull(mdt.propertyFile);
		File f = new File(".");
		mdt.setPropertyFile(f);
		assertSame(f, mdt.propertyFile);
	}
	
	@Test
	public void testSetConfigFile() {
		MetadataTask mdt = new MetadataTask();
		assertNull(mdt.configFile);
		File f = new File(".");
		mdt.setConfigFile(f);
		assertSame(f, mdt.configFile);
	}
	
	@Test
	public void testSetType() {
		MetadataTask mdt = new MetadataTask();
		assertSame(Type.NATIVE, mdt.type);
		mdt.setType("jdbc");
		assertSame(Type.JDBC, mdt.type);
	}
	
	@Test
	public void testAddConfiguredFileSet() {
		MetadataTask mdt = new MetadataTask();
		assertTrue(mdt.fileSets.isEmpty());
		FileSet fs = new FileSet();
		mdt.addConfiguredFileSet(fs);
		assertEquals(1, mdt.fileSets.size());
		assertSame(fs, mdt.fileSets.get(0));
	}

	// TODO HBX-3313: Verify why this does not work on Windows
	@Test
	@DisabledOnOs(OS.WINDOWS)
	public void testCreateNativeMetadataDescriptor() throws Exception {
		String propertiesString = "hibernate.dialect=H2";
		File propertiesFile = new File(tempDir.toFile(), "hibernate.properties");
		Files.write(propertiesFile.toPath(), propertiesString.getBytes());
		String cfgXmlString = 
				"<hibernate-configuration>                " +
				"  <session-factory>                      " +
				"    <property name='foo2'>bar2</property>" +
				"  </session-factory>                     " +
				"</hibernate-configuration>               " ;
		File cfgXmlFile = new File(tempDir.toFile(), "hibernate.cfg.xml");
		Files.write(cfgXmlFile.toPath(), cfgXmlString.getBytes());
		String hbmXmlString = 
				"<hibernate-mapping>     " +
				"  <class name='foobar'/>" +
				"</hibernate-mapping>    " ;
		File hbmFolder = new File(tempDir.toFile(), "hbm");
		hbmFolder.mkdirs();
		File hbmXmlFile = new File(hbmFolder, "foobar.hbm.xml");
		Files.write(hbmXmlFile.toPath(), hbmXmlString.getBytes());
		ArrayList<FileSet> fileSets = new ArrayList<FileSet>();
		FileSet fileSet = new FileSet();
		fileSet.setDir(hbmFolder);
		FilenameSelector filenameSelector = new FilenameSelector();
		filenameSelector.setName("*.hbm.xml");
		fileSet.addFilename(filenameSelector);
		MetadataTask mdt = new MetadataTask();
		mdt.propertyFile = propertiesFile;
		mdt.configFile = cfgXmlFile;
		mdt.fileSets = fileSets;
		MetadataDescriptor metadataDescriptor = mdt.createMetadataDescriptor();
		assertNotNull(metadataDescriptor);
		assertEquals("H2", metadataDescriptor.getProperties().get("hibernate.dialect"));
	}

}
