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
package org.hibernate.tool.hbm2x.GenerateFromJDBC;

import org.hibernate.boot.Metadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.internal.export.doc.DocExporter;
import org.hibernate.tool.internal.export.hbm.HbmExporter;
import org.hibernate.tool.internal.reveng.strategy.AbstractStrategy;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
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
 * @author max
 * @author koen
 */
public class TestCase {
	
	@TempDir
	public File outputDir = new File("output");
	
	private MetadataDescriptor metadataDescriptor = null;
	
	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		AbstractStrategy configurableNamingStrategy = new DefaultStrategy();
		configurableNamingStrategy.setSettings(new RevengSettings(configurableNamingStrategy).setDefaultPackageName("org.reveng").setCreateCollectionForForeignKey(false));
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(configurableNamingStrategy, null);
	}
	
	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testGenerateJava() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);		
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();
		exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.getProperties().setProperty("ejb3", "true");
		exporter.start();
	}
	
	@Test
	public void testGenerateMappings() {
		Exporter exporter = new HbmExporter();	
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();	
		JUnitUtil.assertIsNonEmptyFile(new File(outputDir, "org/reveng/Child.hbm.xml"));
		File file = new File(outputDir, "GeneralHbmSettings.hbm.xml");
        assertFalse(file.exists(), file + " should not exist");
		File[] files = new File[2];
		files[0] = new File(outputDir, "org/reveng/Child.hbm.xml");
		files[1] = new File(outputDir, "org/reveng/Master.hbm.xml");
		Metadata metadata = MetadataDescriptorFactory
				.createNativeDescriptor(null, files, null)
				.createMetadata();
		assertNotNull(metadata.getEntityBinding("org.reveng.Child") );
		assertNotNull(metadata.getEntityBinding("org.reveng.Master") );
	}
	
	@Test
	public void testGenerateCfgXml() throws Exception {	
		Exporter exporter = ExporterFactory.createExporter(ExporterType.CFG);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();				
		JUnitUtil.assertIsNonEmptyFile(new File(outputDir, "hibernate.cfg.xml"));
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(new File(outputDir, "hibernate.cfg.xml"));
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and it has no arguments 
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-configuration/session-factory/mapping")
				.evaluate(document, XPathConstants.NODESET);
		Node[] elements = new Node[nodeList.getLength()];
		for (int i = 0; i < nodeList.getLength(); i++) {
			elements[i] = nodeList.item(i);
		}
		assertEquals(2, elements.length);
        for (Node element : elements) {
            assertNotNull(element.getAttributes().getNamedItem("resource"));
            assertNull(element.getAttributes().getNamedItem("class"));
        }
	}
	
	@Test
	public void testGenerateAnnotationCfgXml() throws Exception {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.CFG);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.getProperties().setProperty("ejb3", "true");
		exporter.start();	
		JUnitUtil.assertIsNonEmptyFile(new File(outputDir, "hibernate.cfg.xml"));
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder  db = dbf.newDocumentBuilder();
		Document document = db.parse(new File(outputDir, "hibernate.cfg.xml"));
		XPath xpath = XPathFactory.newInstance().newXPath();
		// Validate the Generator and it has no arguments 
		NodeList nodeList = (NodeList)xpath
				.compile("//hibernate-configuration/session-factory/mapping")
				.evaluate(document, XPathConstants.NODESET);
		Node[] elements = new Node[nodeList.getLength()];
		for (int i = 0; i < nodeList.getLength(); i++) {
			elements[i] = nodeList.item(i);
		}
		assertEquals(2, elements.length);
        for (Node element : elements) {
            assertNull(element.getAttributes().getNamedItem("resource"));
            assertNotNull(element.getAttributes().getNamedItem("class"));
        }
	}
	
	@Test
	public void testGenerateDoc() {	
		DocExporter exporter = new DocExporter();
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();
		JUnitUtil.assertIsNonEmptyFile(new File(outputDir, "index.html"));
	}
	
	@Test
	public void testPackageNames() {
        for (PersistentClass element : metadataDescriptor
                .createMetadata()
                .getEntityBindings()) {
            assertEquals("org.reveng", StringHelper.qualifier(element.getClassName()));
        }
	}
}
