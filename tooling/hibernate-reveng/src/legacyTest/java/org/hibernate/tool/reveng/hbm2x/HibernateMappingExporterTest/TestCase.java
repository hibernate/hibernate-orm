/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.HibernateMappingExporterTest;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.internal.export.hbm.HbmExporter;
import org.hibernate.tool.reveng.test.utils.ConnectionProvider;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
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
