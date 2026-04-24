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

package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.CompositeElementTest;

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
				"org/hibernate/tool/hbm2x/hbm2hbmxml/CompositeElementTest/Fee.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,  
				"org/hibernate/tool/hbm2x/hbm2hbmxml/CompositeElementTest/Glarch.hbm.xml") );
	}

	@Test
	public void testReadable() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		assertNotNull(db.parse(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/CompositeElementTest/Fee.hbm.xml")));
		assertNotNull(db.parse(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/CompositeElementTest/Glarch.hbm.xml")));
	}
	
	@Test
	public void testCompositeElementNode() throws Exception {
		File outputXml = new File(
        		srcDir, 
        		"org/hibernate/tool/hbm2x/hbm2hbmxml/CompositeElementTest/Glarch.hbm.xml");
        JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/list")
				.evaluate(document, XPathConstants.NODESET);
		Element secondList = (Element)nodeList.item(1);
		NodeList compositeElementList = secondList.getElementsByTagName("composite-element");
		assertEquals(1, compositeElementList.getLength(), "Expected to get one composite-element element");
		Element compositeElement = (Element)compositeElementList.item(0);
		NodeList compositeElementChildNodes = compositeElement.getChildNodes();
		int amountOfProperties = 0;
		for (int i = 0; i < compositeElementChildNodes.getLength(); i++) {
			Node node = compositeElementChildNodes.item(i);
			if ("property".equals(node.getNodeName())) amountOfProperties++;
		}
		assertEquals(2, amountOfProperties, "Expected to get two property element");
		NodeList manyToOneList = secondList.getElementsByTagName("many-to-one");
		assertEquals(1, manyToOneList.getLength());
		Element manyToOneElement = (Element)manyToOneList.item(0);
		assertEquals("fee", manyToOneElement.getAttribute("name"));
		assertEquals("all", manyToOneElement.getAttribute("cascade"));
		NodeList nestedCompositeElementList = compositeElement.getElementsByTagName("nested-composite-element");
		assertEquals(1, nestedCompositeElementList.getLength());
		Element nestedCompositeElement = (Element)nestedCompositeElementList.item(0);
		assertEquals("subcomponent", nestedCompositeElement.getAttribute("name"));
		assertEquals(
				"org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.CompositeElementTest.FooComponent", 
				nestedCompositeElement.getAttribute("class"));
	}

}
