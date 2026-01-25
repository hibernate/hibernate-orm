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

package org.hibernate.tool.hbm2x.hbm2hbmxml.TypeParamsTest;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.internal.export.hbm.HbmExporter;
import org.hibernate.tool.test.utils.ConnectionProvider;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JUnitUtil;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Dmitry Geraskov
 * @author koen
 */
//TODO Reenable this test and make it pass (See HBX-2884)
@Disabled
public class TestCase {
	
	private static final String[] HBM_XML_FILES = new String[] {
			"Order.hbm.xml",
	};
	
	@TempDir
	public File outputFolder = new File("output");
	
	private File srcDir = null;

    @BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "output");
		assertTrue(srcDir.mkdir());
        File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		MetadataDescriptor metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		Exporter hbmexporter = new HbmExporter();
		hbmexporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		hbmexporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		hbmexporter.start();
	}

	@Test
	public void testAllFilesExistence() {
		JUnitUtil.assertIsNonEmptyFile(
				new File(
						srcDir,  
						"org/hibernate/tool/hbm2x/hbm2hbmxml/TypeParamsTest/Order.hbm.xml"));
	}

	@Test
	public void testReadable() {
		File orderHbmXml =
        		new File(
        				srcDir, 
        				"org/hibernate/tool/hbm2x/hbm2hbmxml/TypeParamsTest/Order.hbm.xml");
		Properties properties = new Properties();
		properties.setProperty(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
		properties.setProperty(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
		File[] files = new File[] { orderHbmXml };
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createNativeDescriptor(null, files, properties);
        assertNotNull(metadataDescriptor.createMetadata());
    }

	@Test
	public void testTypeParamsElements() throws Exception {
		File outputXml = new File(
				srcDir,  
				"org/hibernate/tool/hbm2x/hbm2hbmxml/TypeParamsTest/Order.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/property")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(2, nodeList.getLength(), "Expected to get one property element");
		Element statusElement = (Element) nodeList.item(0);
		Element nameElement = (Element) nodeList.item(1);
		if(!statusElement.getAttribute( "name" ).equals("status")) {
			Element temp = nameElement;
			nameElement = statusElement;
			statusElement = temp;
		}
		assertEquals("status", statusElement.getAttribute( "name" ));
		nodeList = statusElement.getElementsByTagName("type");
		assertEquals(1, nodeList.getLength(), "Expected to get one type element");
		nodeList =  ((Element) nodeList.item(0)).getElementsByTagName("param");		
		assertEquals(5, nodeList.getLength(), "Expected to get 5 params elements");
		Map<String, String> params = new HashMap<>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element param = (Element) nodeList.item(i);
			params.put(param.getAttribute( "name" ), param.getTextContent());
		}
		Set<String> set = params.keySet();
		assertEquals(5, params.size(), "Expected to get 5 different params elements");
		assertTrue(
				set.contains("catalog"),
				"Can't find 'catalog' param");
		assertEquals("", params.get("catalog"));
		assertTrue(
				set.contains("column"),
				"Can't find 'column' param");
		assertEquals("STATUS", params.get("column"));
		assertTrue(
				set.contains("table"),
				"Can't find 'table' param");
		assertEquals("ORDERS", params.get("table"));
		assertTrue(
				set.contains("schema"),
				"Can't find 'schema' param");
		assertEquals("", params.get("schema"));
		assertTrue(
				set.contains("enumClass"),
				"Can't find 'enumClass' param");
		assertEquals(
				Status.class.getName(), 
				params.get("enumClass"));
        assertEquals(0, nameElement.getElementsByTagName("type").getLength(), "property name should not have any type element");
		assertEquals("string", nameElement.getAttribute("type"));
	}
	
	enum Status {
		ON, OFF
	}

}
