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

package org.hibernate.tool.hbm2x.hbm2hbmxml.InheritanceTest;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.internal.export.hbm.HbmExporter;
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
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * this test should be fixed to have a proper model. currently a mix of subclass/joinedsubclass is in play.
 * @author max
 *
 */
//TODO Reenable this test and make it pass (See HBX-2884)
@Disabled
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Aliens.hbm.xml"
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
				srcDir,
				"GeneralHbmSettings.hbm.xml").exists() );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/InheritanceTest/Human.hbm.xml"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"org/hibernate/tool/hbm2x/hbm2hbmxml/InheritanceTest/Alien.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir,
				"/org/hibernate/tool/hbm2x/hbm2hbmxml/InheritanceTest/Animal.hbm.xml") );
	}

	@Test
	public void testArtifactCollection() {
		assertEquals(
				3,
				hbmexporter.getArtifactCollector().getFileCount("hbm.xml"));
	}

	@Test
	public void testReadable() {
        ArrayList<File> files = new ArrayList<>(4);
        files.add(new File(
        		srcDir, 
        		"org/hibernate/tool/hbm2x/hbm2hbmxml/InheritanceTest/Alien.hbm.xml"));
        files.add(new File(
        		srcDir, 
        		"org/hibernate/tool/hbm2x/hbm2hbmxml/InheritanceTest/Human.hbm.xml"));
        files.add(new File(
        		srcDir, 
        		"org/hibernate/tool/hbm2x/hbm2hbmxml/InheritanceTest/Animal.hbm.xml"));
		Properties properties = new Properties();
		properties.setProperty(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createNativeDescriptor(null, files.toArray(new File[3]), properties);
        assertNotNull(metadataDescriptor.createMetadata());
    }

	// TODO Re-enable this test: HBX-1247
	@Disabled
	@Test
	public void testComment() throws Exception {
		File outputXml = new File(
				srcDir,
				"/org/hibernate/tool/hbm2x/hbm2hbmxml/InheritanceTest/Alien.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/joined-subclass/comment")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one comment element");
    }
	
	@Test
	public void testDiscriminator() throws Exception {
		File outputXml = new File(
				srcDir, 
				"/org/hibernate/tool/hbm2x/hbm2hbmxml/InheritanceTest/Animal.hbm.xml");
		JUnitUtil.assertIsNonEmptyFile(outputXml);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(outputXml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/discriminator")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one discriminator element");	
		Element node = (Element) nodeList.item(0);
		assertEquals("string", node.getAttribute( "type" ));
	}

}
