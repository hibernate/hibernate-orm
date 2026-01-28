/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.OneToOneTest;

import org.hibernate.cfg.AvailableSettings;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//TODO Reenable this test and make it pass (See HBX-2884)
@Disabled
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"PersonAddressOneToOnePrimaryKey.hbm.xml"
	};

	@TempDir
	public File outputFolder = new File("output");

	private File srcDir = null;
	private HbmExporter hbmexporter = null;

	@BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "src");
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
				srcDir, "GeneralHbmSettings.hbm.xml")
			.exists());
		JUnitUtil.assertIsNonEmptyFile(
				new File(
						srcDir,
						"org/hibernate/tool/hbm2x/hbm2hbmxml/OneToOneTest/Person.hbm.xml"));
		JUnitUtil.assertIsNonEmptyFile(
				new File(
						srcDir,
						"/org/hibernate/tool/hbm2x/hbm2hbmxml/OneToOneTest/Address.hbm.xml"));
	}

	@Test
	public void testArtifactCollection() {
		assertEquals(
				2,
				hbmexporter.getArtifactCollector().getFileCount("hbm.xml"));
	}

	@Test
	public void testReadable() {
		File personHbmXml = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/OneToOneTest/Person.hbm.xml");
		File addressHbmXml = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/OneToOneTest/Address.hbm.xml");
		Properties properties = new Properties();
		properties.put(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
		properties.put(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
		File[] files = new File[] { personHbmXml, addressHbmXml };
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createNativeDescriptor(null, files, properties);
		assertNotNull(metadataDescriptor.createMetadata());
	}

	@Test
	public void testOneToOne() throws Exception {
		File xmlFile = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/OneToOneTest/Person.hbm.xml");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(xmlFile);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/one-to-one")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one-to-one element");
		Element node = (Element) nodeList.item(0);
		assertEquals("address", node.getAttribute( "name" ));
		assertEquals("false", node.getAttribute( "constrained" ));
		xmlFile = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/OneToOneTest/Address.hbm.xml");
		document = db.parse(xmlFile);
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/one-to-one")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one set element");
		node = (Element) nodeList.item(0);
		assertEquals("person", node.getAttribute( "name" ));
		assertEquals("true", node.getAttribute( "constrained" ));
		assertEquals("field", node.getAttribute( "access" ));
	}

}
