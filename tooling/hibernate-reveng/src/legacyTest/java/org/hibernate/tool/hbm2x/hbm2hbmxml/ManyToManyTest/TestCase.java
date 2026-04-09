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

package org.hibernate.tool.hbm2x.hbm2hbmxml.ManyToManyTest;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.internal.export.hbm.HbmExporter;
import org.hibernate.tool.test.utils.ConnectionProvider;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JUnitUtil;
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
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"UserGroup.hbm.xml"
	};
	
	@TempDir
	public File outputFolder = new File("output");
	
	private HbmExporter hbmexporter = null;
	private File srcDir = null;

    @BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "output");
		assertTrue(srcDir.mkdir());
        File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		MetadataDescriptor metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		hbmexporter = new HbmExporter();
		hbmexporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		hbmexporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		hbmexporter.start();
	}

	@Test
	public void testAllFilesExistence() {
		assertFalse(new File(
				srcDir,
				"GeneralHbmSettings.hbm.xml")
			.exists() );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/ManyToManyTest/User.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/ManyToManyTest/Group.hbm.xml") );
	}

	@Test
	public void testArtifactCollection() {
		assertEquals(
				2,
				hbmexporter.getArtifactCollector().getFileCount("hbm.xml"));
	}

	@Test
	public void testReadable() {
        ArrayList<File> files = new ArrayList<>(4);
        files.add(new File(
        		srcDir, 
        		"org/hibernate/tool/hbm2x/hbm2hbmxml/ManyToManyTest/User.hbm.xml"));
        files.add(new File(
        		srcDir, 
        		"org/hibernate/tool/hbm2x/hbm2hbmxml/ManyToManyTest/Group.hbm.xml"));
		Properties properties = new Properties();
		properties.put(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
		properties.put(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createNativeDescriptor(null, files.toArray(new File[2]), properties);
        assertNotNull(metadataDescriptor.createMetadata());
    }

	@Test
	public void testManyToMany() throws Exception {
		File outputXml = new File(
				srcDir,  
				"org/hibernate/tool/hbm2x/hbm2hbmxml/ManyToManyTest/User.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/set/many-to-many")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one many-to-many element");
		Element node = (Element) nodeList.item(0);
		assertEquals("org.hibernate.tool.hbm2x.hbm2hbmxml.ManyToManyTest.Group", node.getAttribute( "entity-name" ));
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/set")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one set element");
		node = (Element) nodeList.item(0);
		assertEquals("UserGroup", node.getAttribute( "table" ));
	}

	@Test
	public void testCompositeId() throws Exception {
		File outputXml = new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/hbm2hbmxml/ManyToManyTest/Group.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one class element");
		Element node = (Element) nodeList.item(0);
		assertEquals("`Group`", node.getAttribute("table"));
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/composite-id")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one composite-id element");
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/composite-id/key-property")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(2, nodeList.getLength(), "Expected to get two key-property elements");
		node = (Element) nodeList.item(0);
		assertEquals("name", node.getAttribute("name"));
		node = (Element) nodeList.item(1);
		assertEquals("org", node.getAttribute("name"));
	}

	@Test
	public void testSetAttributes() throws Exception {
		File outputXml = new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/hbm2hbmxml/ManyToManyTest/Group.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/set")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one set element");
		Element node = (Element) nodeList.item(0);
		assertEquals("UserGroup", node.getAttribute("table"));
		assertEquals("users", node.getAttribute("name"));
		assertEquals("true", node.getAttribute("inverse"));
		assertEquals("extra", node.getAttribute("lazy"));
	}

}
