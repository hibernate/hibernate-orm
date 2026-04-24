/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.Hbm2JavaInitializationTest;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Author.hbm.xml",
			"Article.hbm.xml",
			"Train.hbm.xml",
			"Passenger.hbm.xml"
	};

	@TempDir
	public File outputFolder = new File("output");

	private File srcDir = null;

	@BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "src");
		assertTrue(srcDir.mkdir());
		File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		MetadataDescriptor metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		exporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[0]);
		exporter.start();
	}

	@Test
	public void testFieldInitializationAndTypeNames() throws Exception {
		File articleFile = new File(srcDir,
				"org/hibernate/tool/reveng/hbm2x/Hbm2JavaInitializationTest/Article.java");
		assertTrue(articleFile.exists(), "Article.java should be generated");
		String source = Files.readString(articleFile.toPath());
		// Without jdk5=true, raw types are used for collections
		// Map field: raw Map (no generics)
		assertTrue(source.contains("Map AMap"),
				"AMap should be declared as raw Map");
		assertTrue(source.contains("new HashMap"),
				"AMap should be initialized with new HashMap");
		// List field: raw List
		assertTrue(source.contains("List aList"),
				"aList should be declared as raw List");
		assertTrue(source.contains("new ArrayList"),
				"aList should be initialized with new ArrayList");
		// content: default-value meta attribute
		assertTrue(source.contains("\"what can I say\""),
				"content should be initialized with default-value from meta attribute");
		// bagstrings: raw Collection (bag with element type)
		assertTrue(source.contains("Collection bagstrings"),
				"bagstrings should be declared as raw Collection");
		// naturalSortedArticlesMap: raw Map (sort=natural is SortedMap only when jdk5=true)
		assertTrue(source.contains("naturalSortedArticlesMap"),
				"naturalSortedArticlesMap should be present");
		assertTrue(source.contains("new TreeMap") || source.contains("new HashMap"),
				"sorted maps should be initialized");
		// sortedArticlesSet: raw Set
		assertTrue(source.contains("sortedArticlesSet"),
				"sortedArticlesSet should be present");
		assertTrue(source.contains("new TreeSet") || source.contains("new HashSet"),
				"sorted sets should be initialized");
	}

}
