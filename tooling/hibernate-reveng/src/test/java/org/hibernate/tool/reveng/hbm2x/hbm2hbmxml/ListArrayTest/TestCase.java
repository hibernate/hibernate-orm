/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.ListArrayTest;

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
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/ListArrayTest/Fee.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/ListArrayTest/Glarch.hbm.xml") );
	}

	@Test
	public void testReadable() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		assertNotNull(db.parse(new File(
				srcDir,
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/ListArrayTest/Fee.hbm.xml")));
		assertNotNull(db.parse(new File(
				srcDir,
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/ListArrayTest/Glarch.hbm.xml")));
	}

	@Test
	public void testListNode() throws Exception {
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/ListArrayTest/Glarch.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/list")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(2, nodeList.getLength(), "Expected to get two list element");
		Element node = (Element) nodeList.item(1); //second list
		assertEquals("fooComponents", node.getAttribute( "name" ));
		assertNotEquals("false", node.getAttribute( "lazy" ));
		assertEquals("all", node.getAttribute( "cascade" ));
		nodeList = node.getElementsByTagName("list-index");
		assertEquals(1, nodeList.getLength(), "Expected to get one list-index element");
		Element listIndexNode = (Element) nodeList.item(0);
		assertEquals("tha_indecks", listIndexNode.getAttribute( "column" ));
		node = (Element)listIndexNode.getParentNode();//list
		nodeList = node.getElementsByTagName("composite-element");
		assertEquals(1, nodeList.getLength(), "Expected to get one composite-element element");
		node = (Element)nodeList.item(0);
		int propertyCount = 0;
		nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if ("property".equals(nodeList.item(i).getNodeName())) propertyCount++;
		}
		assertEquals(2, propertyCount, "Expected to get two property element");
		node = (Element)node.getElementsByTagName("many-to-one").item(0);
		assertEquals("fee", node.getAttribute( "name" ));
		assertEquals("all", node.getAttribute( "cascade" ));
		//TODO :assertEquals(node.attribute( "outer-join" ).getText(),"true");
		node = (Element)node.getParentNode();//composite-element
		node = (Element)node.getElementsByTagName("nested-composite-element").item(0);
		assertEquals("subcomponent", node.getAttribute( "name" ));
		assertEquals("org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.ListArrayTest.FooComponent", node.getAttribute( "class" ));
	}

	@Test
	public void testArrayNode() throws Exception {
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/reveng/hbm2x/hbm2hbmxml/ListArrayTest/Glarch.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/array")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one array element");
		Element node = (Element) nodeList.item(0);
		assertEquals("proxyArray", node.getAttribute( "name" ));
		assertEquals("org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.ListArrayTest.GlarchProxy", node.getAttribute( "element-class" ));
		nodeList = node.getElementsByTagName("list-index");
		assertEquals(1, nodeList.getLength(), "Expected to get one list-index element");
		Element listIndexNode = (Element) nodeList.item(0);
		assertEquals("array_indecks", listIndexNode.getAttribute( "column" ));
		nodeList = node.getElementsByTagName("one-to-many");
		assertEquals(1, nodeList.getLength(), "Expected to get one 'one-to-many' element");
	}

}
