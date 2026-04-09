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

package org.hibernate.tool.hbm2x.hbm2hbmxml.Hbm2HbmXmlTest;

import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.export.hbm.HbmExporter;
import org.hibernate.tool.internal.export.hbm.HibernateMappingGlobalSettings;

import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Initial implementation based on the Hbm2XTest class.
 * 
 * @author David Channon
 * @author koen
 */

// TODO Reenable this test and make it pass (See HBX-2884)
@Disabled
public class TestCase {

	/**
	 * Testing class for cfg2hbm generating hbms.
	 * Simulate a custom persister. 
	 * Note: Only needs to exist not work or be valid
	 *       in any other way.
	 * 
	 * @author David Channon
	 */
	public static class Persister {
		// Empty
	}

	private static final String[] HBM_XML_FILES = new String[] {
			"Basic.hbm.xml",
			"BasicCompositeId.hbm.xml",
			"BasicGlobals.hbm.xml",
			"ClassFullAttribute.hbm.xml"
	};
	
	@TempDir
	public File outputFolder = new File("output");
	
	private MetadataDescriptor metadataDescriptor = null;
	private File srcDir = null;
    private HbmExporter hbmexporter = null;

	@BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "src");
		assertTrue(srcDir.mkdir());
        File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		hbmexporter = new HbmExporter();
		hbmexporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		hbmexporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		hbmexporter.start();
	}
	
	// TODO HBX-2042: Reenable when implemented in ORM 6.0
	@Disabled
	@Test
	public void testAllFilesExistence() {
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir, 
				"GeneralHbmSettings.hbm.xml") );
		assertFalse(new File(
				srcDir, 
				"org/hibernate/tool/cfg2hbm/GeneralHbmSettings.hbm.xml").exists() );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/Basic.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/BasicGlobals.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/BasicCompositeId.hbm.xml") );
	}
	
	// TODO HBX-2042: Reenable when implemented in ORM 6.0
	@Disabled
	@Test
	public void testArtifactCollection() {
		assertEquals(
				5,
				hbmexporter.getArtifactCollector().getFileCount("hbm.xml"),
				"4 mappings + 1 global");
	}
	
	@Test
	public void testGlobalSettingsGeneratedDatabase() throws Exception {
		HibernateMappingGlobalSettings hgs = new HibernateMappingGlobalSettings();
		hgs.setDefaultPackage("org.hibernate.tool.hbm2x.hbm2hbmxml.Hbm2HbmXmlTest");
		hgs.setSchemaName("myschema");
		hgs.setCatalogName("mycatalog");		
		HbmExporter gsExporter = new HbmExporter();
		gsExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		gsExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		gsExporter.setGlobalSettings(hgs);
		gsExporter.start();
		File outputXml = new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/BasicGlobals.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		Element root = document.getDocumentElement();
		// There are 7 attributes because there are defaults defined by the DTD makes up the missing entries
		assertEquals(7, root.getAttributes().getLength(), "Unexpected number of hibernate-mapping elements " );
		assertEquals("org.hibernate.tool.hbm2x.hbm2hbmxml.Hbm2HbmXmlTest", root.getAttribute("package"), "Unexpected package name" );
		assertEquals("myschema", root.getAttribute("schema"), "Unexpected schema name" );
		assertEquals("mycatalog", root.getAttribute("catalog"), "Unexpected mycatalog name" );
	}

	@Test
	public void testGlobalSettingsGeneratedAccessAndCascadeNonDefault()  throws Exception {
		HibernateMappingGlobalSettings hgs = new HibernateMappingGlobalSettings();
		hgs.setDefaultPackage("org.hibernate.tool.hbm2x.hbm2hbmxml.Hbm2HbmXmlTest");
		hgs.setDefaultAccess("field");
		hgs.setDefaultCascade("save-update");
		HbmExporter gbsExporter = new HbmExporter();
		gbsExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		gbsExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		gbsExporter.setGlobalSettings(hgs);
		gbsExporter.start();
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/BasicGlobals.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		Element root = document.getDocumentElement();
		// There are 5 attributes because there are non-defaults not set for this test
		assertEquals(5, root.getAttributes().getLength(), "Unexpected number of hibernate-mapping elements " );
		assertEquals("org.hibernate.tool.hbm2x.hbm2hbmxml.Hbm2HbmXmlTest", root.getAttribute("package"), "Unexpected package name" );
		assertEquals("field", root.getAttribute("default-access"), "Unexpected access setting" );
		assertEquals("save-update", root.getAttribute("default-cascade"), "Unexpected cascade setting" );
	}

	@Test
	public void testMetaAttributes() throws Exception {
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/Basic.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/meta")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(2, nodeList.getLength(), "Expected to get one meta element");
		Node node = nodeList.item(0);
		assertEquals("Basic", node.getTextContent());
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/id/meta")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one meta element");
		node = nodeList.item(0);
		assertEquals("basicId", node.getTextContent());
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/property/meta")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one meta element");
		node = nodeList.item(0);
		assertEquals("description", node.getTextContent());
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/set/meta")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one meta element");
		node = nodeList.item(0);
		assertEquals("anotherone", node.getTextContent());
	}

	@Test
	public void testCollectionAttributes() throws Exception {
		File outputXml = new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/Basic.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/set")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one set element");
		Element node = (Element) nodeList.item(0);
		assertEquals("delete, update", node.getAttribute("cascade"));	
	}
	
	@Test
	public void testComments() throws Exception {
		File outputXml = new File(
				srcDir, 
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/ClassFullAttribute.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/comment")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one comment element");
		Node node = nodeList.item(0);
		assertEquals("A comment for ClassFullAttribute", node.getTextContent());
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/property/column/comment")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one comment element");
		node = nodeList.item(0);
		assertEquals("columnd comment", node.getTextContent());
	}

	@Test
	public void testNoComments() throws Exception {
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/Basic.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/comment")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(0, nodeList.getLength(), "Expected to get no comment element");
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/property/column/comment")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(0, nodeList.getLength(), "Expected to get no comment element");	
	}

	@Test
	public void testGlobalSettingsGeneratedAccessAndCascadeDefault()  throws Exception {
		HibernateMappingGlobalSettings hgs = new HibernateMappingGlobalSettings();
		hgs.setDefaultPackage("org.hibernate.tool.hbm2x.hbm2hbmxml.Hbm2HbmXmlTest");
		hgs.setDefaultAccess("property");
		hgs.setDefaultCascade("none");	
		HbmExporter gbsExporter = new HbmExporter();
		gbsExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		gbsExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		gbsExporter.setGlobalSettings(hgs);
		gbsExporter.start();
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/BasicGlobals.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		Element root = document.getDocumentElement();
		// There are 5 attributes because there are non-defaults not set for this test
		assertEquals(5, root.getAttributes().getLength(), "Unexpected number of hibernate-mapping elements " );
		assertEquals("org.hibernate.tool.hbm2x.hbm2hbmxml.Hbm2HbmXmlTest", root.getAttribute("package"), "Unexpected package name" );
		assertEquals("property", root.getAttribute("default-access"), "Unexpected access setting" );
		assertEquals("none", root.getAttribute("default-cascade"), "Unexpected cascade setting" );	
	}

	@Test
	public void testGlobalSettingsLazyAndAutoImportNonDefault()  throws Exception {
		HibernateMappingGlobalSettings hgs = new HibernateMappingGlobalSettings();
		hgs.setDefaultPackage("org.hibernate.tool.hbm2x.hbm2hbmxml.Hbm2HbmXmlTest");
		hgs.setDefaultLazy(false);
		hgs.setAutoImport(false);		
		HbmExporter gbsExporter = new HbmExporter();
		gbsExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		gbsExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		gbsExporter.setGlobalSettings(hgs);
		gbsExporter.start();
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/BasicGlobals.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		Element root = document.getDocumentElement();
		// There are 5 attributes because there are non-defaults not set for this test
		assertEquals(5, root.getAttributes().getLength(), "Unexpected number of hibernate-mapping elements " );
		assertEquals("org.hibernate.tool.hbm2x.hbm2hbmxml.Hbm2HbmXmlTest", root.getAttribute("package"), "Unexpected package name" );
		assertEquals("false", root.getAttribute("default-lazy"), "Unexpected access setting" );
		assertEquals("false", root.getAttribute("auto-import"), "Unexpected cascade setting" );
	}

	@Test
	public void testIdGeneratorHasNotArgumentParameters()  throws Exception {
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/BasicGlobals.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and it has no arguments 
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/id/generator")
				.evaluate(document, XPathConstants.NODESET);
        assertEquals(1, nodeList.getLength(), "Expected to get one generator element");
		Node genAtt = ( (Element)nodeList.item(0)).getAttributeNode("class");
		assertEquals("assigned", genAtt.getTextContent(), "Unexpected generator class name" );
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/id/generator/param")
				.evaluate(document, XPathConstants.NODESET);
        assertEquals(0, nodeList.getLength(), "Expected to get no generator param elements");
	}
    
	@Test
    public void testIdGeneratorHasArgumentParameters()  throws Exception {
		File outputXml = new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/Hbm2HbmXmlTest/Basic.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and that it does have arguments 
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/id/generator")
				.evaluate(document, XPathConstants.NODESET);
        assertEquals(1, nodeList.getLength(), "Expected to get one generator element");
		Node genAtt = ( (Element)nodeList.item(0)).getAttributeNode("class");
		assertEquals("org.hibernate.id.TableHiLoGenerator", genAtt.getTextContent(), "Unexpected generator class name" );
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/id/generator/param")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(2, nodeList.getLength(), "Expected to get correct number of generator param elements" );
		Element tableElement = (Element)nodeList.item(0);
		Attr paramTableAtt = tableElement.getAttributeNode("name");
		Element columnElement = (Element)nodeList.item(1);
		Attr paramColumnAtt = columnElement.getAttributeNode("name");
		if(paramTableAtt.getTextContent().equals("column")) {
			// to make sure the order of the elements doesn't matter.
			Element tempElement = tableElement;
			Attr temp = paramTableAtt;	
			paramTableAtt = paramColumnAtt;
			tableElement = columnElement;
			paramColumnAtt = temp;
			columnElement = tempElement;
		}
		assertEquals("table", paramTableAtt.getTextContent(), "Unexpected generator param name" );
		assertEquals("column", paramColumnAtt.getTextContent(), "Unexpected generator param name" );
		assertEquals("uni_table", tableElement.getTextContent(), "Unexpected param value for table" );
		assertEquals("next_hi_value", columnElement.getTextContent(), "Unexpected param value for column" );
    }

	// TODO HBX-2042: Reenable when implemented in ORM 6.0
	@Disabled
	@Test
	public void testGeneralHbmSettingsQuery()  throws Exception {
		File outputXml = new File(
				srcDir,
				"GeneralHbmSettings.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and that it does have arguments 
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/query")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(2, nodeList.getLength(), "Expected to get correct number of query elements" );
		Attr genAtt = ( (Element)nodeList.item(0) ).getAttributeNode("name");
		assertEquals("test_query_1", genAtt.getTextContent(), "Unexpected query name" );
		genAtt = ( (Element)nodeList.item(0) ).getAttributeNode("flush-mode");
		assertNull(genAtt, "Expected flush-mode value to be null");
		genAtt = ( (Element)nodeList.item(1) ).getAttributeNode("name");
		assertEquals("test_query_2", genAtt.getTextContent(), "Unexpected query name" );
		genAtt = ( (Element)nodeList.item(1) ).getAttributeNode("flush-mode");
		assertEquals("auto", genAtt.getTextContent(), "Unexpected flush-mode value" );
		genAtt = ( (Element)nodeList.item(1) ).getAttributeNode("cacheable");
		assertEquals("true", genAtt.getTextContent(), "Unexpected cacheable value" );
		genAtt = ( (Element)nodeList.item(1) ).getAttributeNode("cache-region");
		assertEquals("myregion", genAtt.getTextContent(), "Unexpected cache-region value" );
		genAtt = ( (Element)nodeList.item(1) ).getAttributeNode("fetch-size");
		assertEquals("10", genAtt.getTextContent(), "Unexpected fetch-size value" );
		genAtt = ( (Element)nodeList.item(1) ).getAttributeNode("timeout");
		assertEquals("1000", genAtt.getTextContent(), "Unexpected timeout value" );
	}

	// TODO HBX-2042: Reenable when implemented in ORM 6.0
	@Disabled
	@Test
	public void testGeneralHbmSettingsSQLQueryBasic()  throws Exception {
		File outputXml = new File(
				srcDir, 
				"GeneralHbmSettings.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and that it does have arguments 
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/sql-query")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(6, nodeList.getLength(), "Expected to get correct number of query elements" );
		nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/sql-query[@name=\"test_sqlquery_1\"]")
				.evaluate(document, XPathConstants.NODESET);
		Element node = (Element)nodeList.item(0);
		assertNotNull(node, "Expected sql-query named 'test_sqlquery_1' not to be null");
		Attr genAtt = node.getAttributeNode("flush-mode");
		assertNull(genAtt, "Expected flush-mode value to be null");
	}
	    
	// TODO HBX-2042: Reenable when implemented in ORM 6.0
	@Disabled
	@Test
	public void testGeneralHbmSettingsSQLQueryAllAttributes()  throws Exception {
		File outputXml = new File(
				srcDir,
				"GeneralHbmSettings.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and that it does have arguments 
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/sql-query[@name=\"test_sqlquery_2\"]")
				.evaluate(document, XPathConstants.NODESET);
		Element node = (Element)nodeList.item(0);
		assertNotNull(node, "Expected sql-query named 'test_sqlquery_2' not to be null");
		Attr genAtt = node.getAttributeNode("name");
		assertEquals("test_sqlquery_2", genAtt.getTextContent(), "Unexpected query name" );
		genAtt = node.getAttributeNode("flush-mode");
		assertEquals("auto", genAtt.getTextContent(), "Unexpected flush-mode value" );
		genAtt = node.getAttributeNode("cacheable");
		assertEquals("true", genAtt.getTextContent(), "Unexpected cacheable value" );
		genAtt = node.getAttributeNode("cache-region");
		assertEquals("myregion", genAtt.getTextContent(), "Unexpected cache-region value" );
		genAtt = node.getAttributeNode("fetch-size");
		assertEquals("10", genAtt.getTextContent(), "Unexpected fetch-size value" );
		genAtt = node.getAttributeNode("timeout");
		assertEquals("1000", genAtt.getTextContent(), "Unexpected timeout value" );
		Element syncTable = (Element)node.getElementsByTagName("synchronize").item(0);
		assertNull(syncTable, "Expected synchronize element to be null");	
	}

	// TODO HBX-2042: Reenable when implemented in ORM 6.0
	@Disabled
	@Test
	public void testGeneralHbmSettingsSQLQuerySynchronize()  throws Exception {
		File outputXml = new File(
				srcDir, 
				"GeneralHbmSettings.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and that it does have arguments 
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/sql-query[@name=\"test_sqlquery_3\"]")
				.evaluate(document, XPathConstants.NODESET);
		Element node = (Element)nodeList.item(0);
		assertNotNull(node, "Expected sql-query named 'test_sqlquery_3' not to be null");
		Attr genAtt = node.getAttributeNode("name");
		assertEquals("test_sqlquery_3", genAtt.getTextContent(), "Unexpected query name" );
		Element syncTable = (Element)node.getElementsByTagName("synchronize").item(0);
		assertNotNull(syncTable, "Expected synchronize element to not be null");
		genAtt = syncTable.getAttributeNode("table");
		assertEquals("mytable", genAtt.getTextContent(), "Unexpected table value for synchronize element" );
		Element returnEl = (Element)node.getElementsByTagName("return").item(0);
		assertNull(returnEl, "Expected return element to be null");
	}

	// TODO HBX-2042: Reenable when implemented in ORM 6.0
	@Disabled
	@Test
	public void testGeneralHbmSettingsSQLQueryWithReturnRoot()  throws Exception {
		File outputXml = new File(
				srcDir, 
				"GeneralHbmSettings.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and that it does have arguments 
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/sql-query[@name=\"test_sqlquery_4\"]")
				.evaluate(document, XPathConstants.NODESET);
		Element node = (Element)nodeList.item(0);
		assertNotNull(node, "Expected sql-query named 'test_sqlquery_4' not to be null");
		Attr genAtt = node.getAttributeNode("name");
		assertEquals("test_sqlquery_4", genAtt.getTextContent(), "Unexpected query name" );
		Element returnEl = (Element)node.getElementsByTagName("return").item(0);
		assertNotNull(returnEl, "Expected return element to not be null");
		genAtt = returnEl.getAttributeNode("alias");
		assertEquals("e", genAtt.getTextContent(), "Unexpected alias value for return element" );
		genAtt = returnEl.getAttributeNode("class");
		assertEquals("org.hibernate.tool.hbm2x.hbm2hbmxml.BasicGlobals", genAtt.getTextContent(), "Unexpected class value for return element");
	}

	// TODO HBX-2042: Reenable when implemented in ORM 6.0
	@Disabled
	@Test
	public void testGeneralHbmSettingsSQLQueryWithReturnRole()  throws Exception {
		File outputXml = new File(
				srcDir, 
				"GeneralHbmSettings.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and that it does have arguments 
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/sql-query[@name=\"test_sqlquery_5\"]")
				.evaluate(document, XPathConstants.NODESET);
		Element node = (Element)nodeList.item(0);
		assertNotNull(node, "Expected sql-query named 'test_sqlquery_5' not to be null");
		Attr genAtt = node.getAttributeNode("name");
		assertEquals("test_sqlquery_5", genAtt.getTextContent(), "Unexpected query name" );
		Element returnEl = (Element)node.getElementsByTagName("return-join").item(0);
		assertNotNull(returnEl, "Expected return element to not be null");
		genAtt = returnEl.getAttributeNode("alias");
		assertEquals("e", genAtt.getTextContent(), "Unexpected alias value for return element" );
		genAtt = returnEl.getAttributeNode("property");
		assertEquals("e.age", genAtt.getTextContent(), "Unexpected property role value for return element");
	}
	    
	// TODO HBX-2042: Reenable when implemented in ORM 6.0
	@Disabled
	@Test
	public void testGeneralHbmSettingsSQLQueryWithReturnCollection()  throws Exception {
		File outputXml = new File(
				srcDir, 
				"GeneralHbmSettings.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and that it does have arguments 
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/sql-query[@name=\"test_sqlquery_6\"]")
				.evaluate(document, XPathConstants.NODESET);
		Element node = (Element)nodeList.item(0);
		assertNotNull(node, "Expected sql-query named 'test_sqlquery_6' not to be null");
		Attr genAtt = node.getAttributeNode("name");
		assertEquals("test_sqlquery_6", genAtt.getTextContent(), "Unexpected query name");
		Element returnEl = (Element)node.getElementsByTagName("load-collection").item(0);
		assertNotNull(returnEl, "Expected return element to not be null");
		genAtt = returnEl.getAttributeNode("alias");
		assertEquals("e", genAtt.getTextContent(), "Unexpected alias value for return element");
		genAtt = returnEl.getAttributeNode("role");
		assertEquals("org.hibernate.tool.hbm2x.hbm2hbmxml.Hbm2HbmXmlTest.BasicGlobals.price", genAtt.getTextContent(), "Unexpected collection role value for return element");
		genAtt = returnEl.getAttributeNode("lock-mode");
		assertEquals("none", genAtt.getTextContent(), "Unexpected class lock-mode for return element");
	}
	
}
