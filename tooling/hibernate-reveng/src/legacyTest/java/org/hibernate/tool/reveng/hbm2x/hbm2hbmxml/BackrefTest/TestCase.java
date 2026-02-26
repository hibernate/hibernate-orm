/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.BackrefTest;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.internal.export.hbm.HbmExporter;
import org.hibernate.tool.reveng.test.utils.ConnectionProvider;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmitry Geraskov
 * @author koen
 */
//TODO Reenable this test and make it pass (See HBX-2884)
@Disabled
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Car.hbm.xml"
	};

	@TempDir
	public File outputFolder = new File("output");

	private Metadata metadata = null;
	private File srcDir = null;

	@BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "src");
		assertTrue(srcDir.mkdir());
		File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		MetadataDescriptor metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		metadata = metadataDescriptor.createMetadata();
		Exporter hbmexporter = new HbmExporter();
		hbmexporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		hbmexporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		hbmexporter.start();
	}

	@Test
	public void testAllFilesExistence() {
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/BackrefTest/Car.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/BackrefTest/CarPart.hbm.xml") );
	}

	@Test
	public void testBackrefPresent() {
		PersistentClass pc = metadata.getEntityBinding("org.hibernate.tool.hbm2x.hbm2hbmxml.BackrefTest.CarPart");
		Iterator<Property> iterator = pc.getProperties().iterator();
		boolean hasBackrefs = false;
		while (iterator.hasNext() && !hasBackrefs) {
			hasBackrefs = (iterator.next() instanceof Backref);
		}
		assertTrue(hasBackrefs, "Class mapping should create Backref for this testcase");
	}

	@Test
	public void testReadable() {
		ArrayList<File> files = new ArrayList<>(4);
		files.add(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/BackrefTest/Car.hbm.xml"));
		files.add(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/BackrefTest/CarPart.hbm.xml"));
		Properties properties = new Properties();
		properties.put(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
		properties.put(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createNativeDescriptor(null, files.toArray(new File[2]), properties);
		assertNotNull(metadataDescriptor.createMetadata());
	}

}
