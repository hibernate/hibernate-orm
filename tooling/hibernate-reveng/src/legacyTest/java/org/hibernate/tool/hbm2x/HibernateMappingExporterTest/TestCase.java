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

package org.hibernate.tool.hbm2x.HibernateMappingExporterTest;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.internal.export.hbm.HbmExporter;
import org.hibernate.tool.test.utils.ConnectionProvider;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//TODO Reenable this test and make it pass (See HBX-2884)
@Disabled
public class TestCase {
	
	private static final String FOO_HBM_XML = 
			"<hibernate-mapping>              "+
			"	<class name='Foo' table='FOO'>"+
			"		<id type='string'>        "+
			"			<column name='BAR'/>  "+
			"		</id>                     "+
			"	</class>                      "+
			"</hibernate-mapping>             ";

	@TempDir
	public File outputFolder = new File("output");
	
	@Test
	public void testStart() throws Exception {
		File resources = new File(outputFolder, "resources");
		assertTrue(resources.mkdir());
		File fooHbmXmlOrigin = new File(resources, "origin.hbm.xml");
		FileWriter writer = new FileWriter(fooHbmXmlOrigin);
		writer.write(FOO_HBM_XML);
		writer.close();
		Properties properties = new Properties();
		properties.put(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
		properties.put(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createNativeDescriptor(null, new File[] { fooHbmXmlOrigin }, properties); 		
		final File srcDir = new File(outputFolder, "output");
		assertTrue(srcDir.mkdir());
		HbmExporter exporter = new HbmExporter();
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		final File fooHbmXml = new File(srcDir, "Foo.hbm.xml");
		assertFalse( fooHbmXml.exists());
		exporter.start();
		assertTrue(fooHbmXml.exists());
		assertTrue(fooHbmXml.delete());
	}

}
