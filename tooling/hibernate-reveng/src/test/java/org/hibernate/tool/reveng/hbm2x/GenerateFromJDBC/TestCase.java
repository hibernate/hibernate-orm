/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.GenerateFromJDBC;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.reveng.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.reveng.RevengSettings;
import org.hibernate.tool.reveng.internal.strategy.AbstractStrategy;
import org.hibernate.tool.reveng.internal.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	@TempDir
	public File outputDir = new File("output");

	private MetadataDescriptor metadataDescriptor = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		AbstractStrategy configurableNamingStrategy = new DefaultStrategy();
		configurableNamingStrategy.setSettings(new RevengSettings(configurableNamingStrategy).setDefaultPackageName("org.reveng").setCreateCollectionForForeignKey(false));
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(configurableNamingStrategy, null);
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testGenerateJava() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();
		exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.getProperties().setProperty("ejb3", "true");
		exporter.start();
	}

	@Test
	public void testGenerateMappings() throws Exception {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.HBM);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();
		JUnitUtil.assertIsNonEmptyFile(new File(outputDir, "org/reveng/Child.hbm.xml"));
		File file = new File(outputDir, "GeneralHbmSettings.hbm.xml");
		assertFalse(file.exists(), file + " should not exist");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document childDoc = db.parse(new File(outputDir, "org/reveng/Child.hbm.xml"));
		assertNotNull(childDoc);
		Document masterDoc = db.parse(new File(outputDir, "org/reveng/Master.hbm.xml"));
		assertNotNull(masterDoc);
	}

	@Test
	public void testGenerateCfgXml() throws Exception {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.CFG);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();
		JUnitUtil.assertIsNonEmptyFile(new File(outputDir, "hibernate.cfg.xml"));
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(new File(outputDir, "hibernate.cfg.xml"));
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and it has no arguments
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-configuration/session-factory/mapping")
				.evaluate(document, XPathConstants.NODESET);
		Node[] elements = new Node[nodeList.getLength()];
		for (int i = 0; i < nodeList.getLength(); i++) {
			elements[i] = nodeList.item(i);
		}
		assertEquals(2, elements.length);
		for (Node element : elements) {
			assertNotNull(element.getAttributes().getNamedItem("resource"));
			assertNull(element.getAttributes().getNamedItem("class"));
		}
	}

	@Test
	public void testGenerateAnnotationCfgXml() throws Exception {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.CFG);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.getProperties().setProperty("ejb3", "true");
		exporter.start();
		JUnitUtil.assertIsNonEmptyFile(new File(outputDir, "hibernate.cfg.xml"));
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(new File(outputDir, "hibernate.cfg.xml"));
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and it has no arguments
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-configuration/session-factory/mapping")
				.evaluate(document, XPathConstants.NODESET);
		Node[] elements = new Node[nodeList.getLength()];
		for (int i = 0; i < nodeList.getLength(); i++) {
			elements[i] = nodeList.item(i);
		}
		assertEquals(2, elements.length);
		for (Node element : elements) {
			assertNull(element.getAttributes().getNamedItem("resource"));
			assertNotNull(element.getAttributes().getNamedItem("class"));
		}
	}

	@Test
	public void testGenerateDoc() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.DOC);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();
		JUnitUtil.assertIsNonEmptyFile(new File(outputDir, "index.html"));
	}

	@Test
	public void testPackageNames() {
		for (ClassDetails entity : ((RevengMetadataDescriptor) metadataDescriptor)
				.getEntityClassDetails()) {
			assertEquals("org.reveng", StringHelper.qualifier(entity.getClassName()));
		}
	}
}
