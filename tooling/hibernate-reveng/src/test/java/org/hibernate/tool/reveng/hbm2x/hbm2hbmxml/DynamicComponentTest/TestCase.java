/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.DynamicComponentTest;

import org.hibernate.tool.reveng.api.export.ArtifactCollector;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.export.DefaultArtifactCollector;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Dmitry Geraskov
 * @author koen
 */
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Glarch.hbm.xml"
	};

	@TempDir
	public File outputFolder = new File("output");

	private File srcDir = null;

	private ArtifactCollector artifactCollector = new DefaultArtifactCollector();

	@BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "src");
		assertTrue(srcDir.mkdir());
		File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		MetadataDescriptor metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		Exporter hbmexporter = ExporterFactory.createExporter(ExporterType.HBM);
		hbmexporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		hbmexporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		hbmexporter.getProperties().put(ExporterConstants.ARTIFACT_COLLECTOR, artifactCollector);
		hbmexporter.start();
	}

	@Test
	public void testAllFilesExistence() {
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/DynamicComponentTest/Fee.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/DynamicComponentTest/Glarch.hbm.xml") );
	}

	@Test
	public void testReadable() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		assertNotNull(db.parse(new File(
				srcDir,
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/DynamicComponentTest/Fee.hbm.xml")));
		assertNotNull(db.parse(new File(
				srcDir,
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/DynamicComponentTest/Glarch.hbm.xml")));
	}

	@Test
	public void testClassProxy() throws Exception {
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/DynamicComponentTest/Glarch.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one class element");
		Element element = (Element) nodeList.item(0);
		assertEquals("org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.DynamicComponentTest.GlarchProxy", element.getAttribute("proxy"));
	}

	@Test
	public void testDynamicComponentNode() throws Exception {
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/DynamicComponentTest/Glarch.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/dynamic-component")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one dynamic-component element");
		Element element = (Element) nodeList.item(0);
		assertEquals("dynaBean", element.getAttribute( "name" ));
	}

}
