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
package org.hibernate.tool.hbm2x.OtherCfg2HbmTest;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.internal.export.hbm.HbmExporter;
import org.hibernate.tool.test.utils.ConnectionProvider;
import org.hibernate.tool.test.utils.FileUtil;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
//TODO Reenable this test and make it pass (See HBX-2884)
@Disabled
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Customer.hbm.xml",
			"Order.hbm.xml",
			"LineItem.hbm.xml",
			"Product.hbm.xml",
			"HelloWorld.hbm.xml"
	};
	
	@TempDir
	public File outputFolder = new File("output");
	
	private File srcDir = null;

    @BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "src");
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
	public void testFileExistence() {
		JUnitUtil.assertIsNonEmptyFile(new File(srcDir, "org/hibernate/tool/hbm2x/Customer.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(srcDir, "org/hibernate/tool/hbm2x/LineItem.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(srcDir, "org/hibernate/tool/hbm2x/Order.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(srcDir, "org/hibernate/tool/hbm2x/Product.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(srcDir, "HelloWorld.hbm.xml") );
		JUnitUtil.assertIsNonEmptyFile(new File(srcDir, "HelloUniverse.hbm.xml") );		
	}
	
	@Test
    public void testReadable() {
		Properties properties = new Properties();
		properties.put(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
		properties.put(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
        File[] hbmFiles = new File[4];
        hbmFiles[0] = new File(srcDir, "org/hibernate/tool/hbm2x/Customer.hbm.xml");
        hbmFiles[1] = new File(srcDir, "org/hibernate/tool/hbm2x/LineItem.hbm.xml");
        hbmFiles[2] = new File(srcDir, "org/hibernate/tool/hbm2x/Order.hbm.xml");
        hbmFiles[3] = new File(srcDir, "org/hibernate/tool/hbm2x/Product.hbm.xml");       
        Metadata metadata = MetadataDescriptorFactory
        		.createNativeDescriptor(null, hbmFiles, properties)
        		.createMetadata();
        assertNotNull(metadata);      
    }
	
	@Test
	public void testNoVelocityLeftOvers() {
        assertNull(FileUtil.findFirstString("$", new File(srcDir, "org/hibernate/tool/hbm2x/Customer.hbm.xml")));
        assertNull(FileUtil.findFirstString("$", new File(srcDir, "org/hibernate/tool/hbm2x/LineItem.hbm.xml")));
        assertNull(FileUtil.findFirstString("$", new File(srcDir, "org/hibernate/tool/hbm2x/Order.hbm.xml")));
        assertNull(FileUtil.findFirstString("$", new File(srcDir, "org/hibernate/tool/hbm2x/Product.hbm.xml")));
	}
	
	@Test
	public void testVersioning() throws Exception {	
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(new File(srcDir, "org/hibernate/tool/hbm2x/Product.hbm.xml"));
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-mapping/class/version")
				.evaluate(document, XPathConstants.NODESET);
		assertEquals(1, nodeList.getLength(), "Expected to get one version element");			
 	}
	
}
