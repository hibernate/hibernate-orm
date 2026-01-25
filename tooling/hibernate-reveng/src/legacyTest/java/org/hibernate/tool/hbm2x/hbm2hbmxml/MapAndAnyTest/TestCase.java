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
package org.hibernate.tool.hbm2x.hbm2hbmxml.MapAndAnyTest;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.internal.export.hbm.HbmExporter;
import org.hibernate.tool.test.utils.ConnectionProvider;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.jspecify.annotations.NonNull;
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
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Dmitry Geraskov
 * @author koen
 */
//TODO Reenable this test and make it pass (See HBX-2884)
@Disabled
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Properties.hbm.xml",
			"Person.hbm.xml"
	};

	@TempDir
	public File outputFolder = new File("output");

    private File srcDir = null;
    private Metadata metadata = null;

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
	public void testReadable() {
		ArrayList<File> files = getFiles();
		Properties properties = new Properties();
		properties.put(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
		properties.put(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createNativeDescriptor(null, files.toArray(new File[4]), properties);
        assertNotNull(metadataDescriptor.createMetadata());
    }

	private @NonNull ArrayList<File> getFiles() {
		ArrayList<File> files = new ArrayList<>(4);
		files.add(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/ComplexPropertyValue.hbm.xml"));
		files.add(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/IntegerPropertyValue.hbm.xml"));
		files.add(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/StringPropertyValue.hbm.xml"));
		files.add(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/MapAndAnyTest/PropertySet.hbm.xml"));
		return files;
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
		} else if (className.indexOf("StringPropertyValue") > 0){
			assertEquals("S", node.getAttribute( "value" ));
		} else {
			assertTrue(className.indexOf("ComplexPropertyValue") > 0);
			assertEquals("C", node.getAttribute( "value" ));
		}
	}

	@Test
	public void testMetaValueRead() {
		PersistentClass pc = metadata.getEntityBinding("org.hibernate.tool.hbm2x.hbm2hbmxml.MapAndAnyTest.Person");
		assertNotNull(pc);
		Property prop = pc.getProperty("data");
		assertNotNull(prop);
        assertInstanceOf(Any.class, prop.getValue());
		Any any = (Any) prop.getValue();
        assertNotNull(any.getMetaValues(), "Expected to get one meta-value element");
		assertEquals(1, any.getMetaValues().size(), "Expected to get one meta-value element");
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
		assertEquals("true", node.getAttribute( "lazy" ));
		assertEquals("all", node.getAttribute( "cascade" ));
		assertEquals("field", node.getAttribute( "access" ));
		nodeList = node.getElementsByTagName("key");
		assertEquals(1, nodeList.getLength(), "Expected to get one key element");
		nodeList = node.getElementsByTagName("map-key");
		assertEquals(1, nodeList.getLength(), "Expected to get one map-key element");
		node = (Element) nodeList.item(0);
		assertEquals("string", node.getAttribute( "type" ));
		nodeList = node.getElementsByTagName("column");
		assertEquals(1, nodeList.getLength(), "Expected to get one column element");
		node = (Element)node.getParentNode();//map
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
		} else {
			assertTrue(className.indexOf("StringPropertyValue") > 0);
			assertEquals("S", node.getAttribute( "value" ));
		}
	}

}
