/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.MapAndAnyTest;

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
			"Properties.hbm.xml",
			"Person.hbm.xml"
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
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/ComplexPropertyValue.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/IntegerPropertyValue.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/StringPropertyValue.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/PropertySet.hbm.xml") );
	}

	@Test
	public void testReadable() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		assertNotNull(db.parse(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/ComplexPropertyValue.hbm.xml")));
		assertNotNull(db.parse(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/IntegerPropertyValue.hbm.xml")));
		assertNotNull(db.parse(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/StringPropertyValue.hbm.xml")));
		assertNotNull(db.parse(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/PropertySet.hbm.xml")));
	}

	@Test
	public void testAnyNode() throws Exception {
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/PropertySet.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/any")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one any element");
		Element node = (Element) nodeList.item(0);
		assertEquals("someSpecificProperty", node.getAttribute( "name" ));
		assertEquals("long", node.getAttribute( "id-type" ));
		assertEquals("string", node.getAttribute( "meta-type" ));
		assertEquals("all", node.getAttribute( "cascade" ));
		assertEquals("field", node.getAttribute( "access" ));
		nodeList = node.getElementsByTagName("column");
		assertEquals(2, nodeList.getLength(), "Expected to get two column elements");
		nodeList = node.getElementsByTagName("meta-value");
		assertEquals(3, nodeList.getLength(), "Expected to get three meta-value elements");
		node = (Element) nodeList.item(0);
		String className = node.getAttribute( "class" );
		assertNotNull(className, "Expected class attribute in meta-value");
		if (className.indexOf("IntegerPropertyValue") > 0){
			assertEquals("I", node.getAttribute( "value" ));
		}
		else if (className.indexOf("StringPropertyValue") > 0){
			assertEquals("S", node.getAttribute( "value" ));
		}
		else {
			assertTrue(className.indexOf("ComplexPropertyValue") > 0);
			assertEquals("C", node.getAttribute( "value" ));
		}
	}

	@Test
	public void testMetaValueRead() throws Exception {
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/Person.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/any[@name='data']")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one any element for 'data'");
		Element anyNode = (Element) nodeList.item(0);
		NodeList metaValues = anyNode.getElementsByTagName("meta-value");
		assertEquals(1, metaValues.getLength(), "Expected to get one meta-value element");
	}

	@Test
	public void testMapManyToAny() throws Exception {
		File outputXml = new File(srcDir,  "org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/PropertySet.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/map")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one any element");
		Element node = (Element) nodeList.item(0);
		assertEquals("generalProperties", node.getAttribute( "name" ));
		assertEquals("T_GEN_PROPS", node.getAttribute( "table" ));
		// lazy="true" is the default for collections; may or may not be rendered
		assertNotEquals("false", node.getAttribute( "lazy" ));
		assertEquals("all", node.getAttribute( "cascade" ));
		assertEquals("field", node.getAttribute( "access" ));
		nodeList = node.getElementsByTagName("key");
		assertEquals(1, nodeList.getLength(), "Expected to get one key element");
		nodeList = node.getElementsByTagName("map-key");
		assertEquals(1, nodeList.getLength(), "Expected to get one map-key element");
		Element mapKeyNode = (Element) nodeList.item(0);
		assertEquals("string", mapKeyNode.getAttribute( "type" ));
		assertEquals("GEN_PROP_NAME", mapKeyNode.getAttribute( "column" ));
		nodeList = node.getElementsByTagName("many-to-any");
		assertEquals(1, nodeList.getLength(), "Expected to get one many-to-any element");
		node = (Element) nodeList.item(0);
		nodeList = node.getElementsByTagName("column");
		assertEquals(2, nodeList.getLength(), "Expected to get two column elements");
		nodeList = node.getElementsByTagName("meta-value");
		assertEquals(2, nodeList.getLength(), "Expected to get two meta-value elements");
		node = (Element) nodeList.item(0);
		String className = node.getAttribute( "class" );
		assertNotNull(className, "Expected class attribute in meta-value");
		if (className.indexOf("IntegerPropertyValue") > 0){
			assertEquals("I", node.getAttribute( "value" ));
		}
		else {
			assertTrue(className.indexOf("StringPropertyValue") > 0);
			assertEquals("S", node.getAttribute( "value" ));
		}
	}

}
