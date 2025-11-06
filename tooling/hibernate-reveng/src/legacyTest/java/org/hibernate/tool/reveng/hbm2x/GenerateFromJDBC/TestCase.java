/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.GenerateFromJDBC;

import org.hibernate.boot.Metadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.core.RevengSettings;
import org.hibernate.tool.reveng.internal.export.doc.DocExporter;
import org.hibernate.tool.reveng.internal.export.hbm.HbmExporter;
import org.hibernate.tool.reveng.internal.core.strategy.AbstractStrategy;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
	public void testGenerateMappings() {
		Exporter exporter = new HbmExporter();
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();
		JUnitUtil.assertIsNonEmptyFile(new File(outputDir, "org/reveng/Child.hbm.xml"));
		File file = new File(outputDir, "GeneralHbmSettings.hbm.xml");
		assertFalse(file.exists(), file + " should not exist");
		File[] files = new File[2];
		files[0] = new File(outputDir, "org/reveng/Child.hbm.xml");
		files[1] = new File(outputDir, "org/reveng/Master.hbm.xml");
		Metadata metadata = MetadataDescriptorFactory
				.createNativeDescriptor(null, files, null)
				.createMetadata();
		assertNotNull(metadata.getEntityBinding("org.reveng.Child") );
		assertNotNull(metadata.getEntityBinding("org.reveng.Master") );
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
		DocExporter exporter = new DocExporter();
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();
		JUnitUtil.assertIsNonEmptyFile(new File(outputDir, "index.html"));
	}

	@Test
	public void testPackageNames() {
		for (PersistentClass element : metadataDescriptor
				.createMetadata()
				.getEntityBindings()) {
			assertEquals("org.reveng", StringHelper.qualifier(element.getClassName()));
		}
	}
}
