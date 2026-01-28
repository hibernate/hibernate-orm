/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.PropertiesTest;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.export.common.DefaultArtifactCollector;
import org.hibernate.tool.reveng.internal.export.hbm.HbmExporter;
import org.hibernate.tool.reveng.test.utils.FileUtil;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JavaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Josh Moore josh.moore@gmx.de
 * @author koen
 */
//TODO Reenable this test and make it pass (See HBX-2884)
@Disabled
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Properties.hbm.xml"
	};

	@TempDir
	public File outputFolder = new File("output");

	private DefaultArtifactCollector artifactCollector;
	private File outputDir = null;

	@BeforeEach
	public void setUp() throws Exception {
		artifactCollector = new DefaultArtifactCollector();
		outputDir = new File(outputFolder, "src");
		assertTrue(outputDir.mkdir());
		File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		MetadataDescriptor metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.getProperties().put(ExporterConstants.ARTIFACT_COLLECTOR, artifactCollector);
		Exporter hbmexporter = new HbmExporter();
		hbmexporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		hbmexporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		hbmexporter.getProperties().put(ExporterConstants.ARTIFACT_COLLECTOR, artifactCollector);
		exporter.start();
		hbmexporter.start();
	}

	@Test
	public void testNoGenerationOfEmbeddedPropertiesComponent() {
		assertEquals(2, artifactCollector.getFileCount("java"));
		assertEquals(2, artifactCollector.getFileCount("hbm.xml"));
	}

	@Test
	public void testGenerationOfEmbeddedProperties() throws Exception {
		File outputXml = new File(outputDir,  "properties/PPerson.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/properties")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one properties element");
		Element element = (Element) nodeList.item(0);
		assertEquals("emergencyContact", element.getAttribute( "name" ));
		assertNotNull(
				FileUtil.findFirstString(
						"name",
						new File(outputDir, "properties/PPerson.java" )));
		assertNull(
				FileUtil.findFirstString(
						"emergencyContact",
						new File(outputDir, "properties/PPerson.java" )),
				"Embedded component/properties should not show up in .java");
	}

	@Test
	public void testCompilable() throws Exception {
		String propertiesUsageResourcePath = "/org/hibernate/tool/hbm2x/PropertiesTest/PropertiesUsage.java_";
		File propertiesUsageOrigin = new File(Objects.requireNonNull(getClass().getResource(propertiesUsageResourcePath)).toURI());
		File propertiesUsageDestination = new File(outputDir, "properties/PropertiesUsage.java");
		File targetDir = new File(outputFolder, "target" );
		assertTrue(targetDir.mkdir());
		Files.copy(propertiesUsageOrigin.toPath(), propertiesUsageDestination.toPath());
		JavaUtil.compile(outputDir, targetDir);
		assertTrue(new File(targetDir, "properties/PCompany.class").exists());
		assertTrue(new File(targetDir, "properties/PPerson.class").exists());
		assertTrue(new File(targetDir, "properties/PropertiesUsage.class").exists());
	}

}
