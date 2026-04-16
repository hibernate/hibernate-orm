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
package org.hibernate.tool.jdbc2cfg.MetaAttributes;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full meta-attribute pipeline:
 * database -> reveng strategy (with meta-attributes) -> exporter -> generated output.
 *
 * @author koen
 */
public class TestCase {

	private static final String REVENG_XML =
			"org/hibernate/tool/jdbc2cfg/MetaAttributes/reveng.xml";

	@TempDir
	public File outputFolder = new File("output");

	private MetadataDescriptor metadataDescriptor = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		RevengStrategy strategy = new OverrideRepository()
				.addResource(REVENG_XML)
				.getReverseEngineeringStrategy(new DefaultStrategy());
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(strategy, null);
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testClassMetaAttributesInDescriptor() {
		RevengMetadataDescriptor revengDescriptor =
				(RevengMetadataDescriptor) metadataDescriptor;
		Map<String, Map<String, List<String>>> allClassMeta =
				revengDescriptor.getAllClassMetaAttributes();
		assertFalse(allClassMeta.isEmpty(),
				"Class meta-attributes should not be empty");
		// Find the entry for MetaProduct
		Map<String, List<String>> productMeta = findClassMeta(allClassMeta, "MetaProduct");
		assertNotNull(productMeta,
				"MetaProduct should have class-level meta-attributes");
		assertTrue(productMeta.containsKey("class-description"));
		assertEquals("The product entity",
				productMeta.get("class-description").get(0));
		assertTrue(productMeta.containsKey("custom-tag"));
		assertEquals("custom-value",
				productMeta.get("custom-tag").get(0));
	}

	@Test
	public void testFieldMetaAttributesInDescriptor() {
		RevengMetadataDescriptor revengDescriptor =
				(RevengMetadataDescriptor) metadataDescriptor;
		Map<String, Map<String, Map<String, List<String>>>> allFieldMeta =
				revengDescriptor.getAllFieldMetaAttributes();
		assertFalse(allFieldMeta.isEmpty(),
				"Field meta-attributes should not be empty");
		// Find the entry for MetaProduct
		Map<String, Map<String, List<String>>> productFieldMeta =
				findFieldMeta(allFieldMeta, "MetaProduct");
		assertNotNull(productFieldMeta,
				"MetaProduct should have field-level meta-attributes");
		Map<String, List<String>> descriptionMeta = productFieldMeta.get("description");
		assertNotNull(descriptionMeta,
				"description field should have meta-attributes");
		assertTrue(descriptionMeta.containsKey("field-description"));
		assertEquals("The product description",
				descriptionMeta.get("field-description").get(0));
	}

	@Test
	public void testHbmExportClassMeta() throws Exception {
		Exporter hbmExporter = ExporterFactory.createExporter(ExporterType.HBM);
		hbmExporter.getProperties().put(
				ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		hbmExporter.getProperties().put(
				ExporterConstants.DESTINATION_FOLDER, outputFolder);
		hbmExporter.start();

		File hbmFile = new File(outputFolder, "MetaProduct.hbm.xml");
		assertTrue(hbmFile.exists(), "MetaProduct.hbm.xml should be generated");

		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(hbmFile);
		XPath xpath = XPathFactory.newInstance().newXPath();

		// Verify class-level <meta> elements
		NodeList classMetas = (NodeList) xpath.evaluate(
				"//hibernate-mapping/class/meta", doc, XPathConstants.NODESET);
		assertTrue(classMetas.getLength() >= 2,
				"Should have at least 2 class-level <meta> elements");
		assertMetaElement(classMetas, "class-description", "The product entity");
		assertMetaElement(classMetas, "custom-tag", "custom-value");

		// Verify field-level <meta> on the description property
		NodeList propertyMetas = (NodeList) xpath.evaluate(
				"//hibernate-mapping/class/property[@name='description']/meta",
				doc, XPathConstants.NODESET);
		assertEquals(1, propertyMetas.getLength(),
				"description property should have 1 <meta> element");
		Element meta = (Element) propertyMetas.item(0);
		assertEquals("field-description", meta.getAttribute("attribute"));
		assertEquals("The product description", meta.getTextContent());

		// Verify the name property has no <meta> elements
		NodeList nameMetas = (NodeList) xpath.evaluate(
				"//hibernate-mapping/class/property[@name='name']/meta",
				doc, XPathConstants.NODESET);
		assertEquals(0, nameMetas.getLength(),
				"name property should have no <meta> elements");
	}

	private void assertMetaElement(NodeList metas, String attribute, String value) {
		for (int i = 0; i < metas.getLength(); i++) {
			Element el = (Element) metas.item(i);
			if (attribute.equals(el.getAttribute("attribute"))
					&& value.equals(el.getTextContent())) {
				return;
			}
		}
		fail("Expected <meta attribute=\"" + attribute + "\">" + value
				+ "</meta> not found");
	}

	private Map<String, List<String>> findClassMeta(
			Map<String, Map<String, List<String>>> allClassMeta, String entityName) {
		for (Map.Entry<String, Map<String, List<String>>> entry : allClassMeta.entrySet()) {
			if (entry.getKey().contains(entityName) || entry.getKey().equalsIgnoreCase(entityName)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private Map<String, Map<String, List<String>>> findFieldMeta(
			Map<String, Map<String, Map<String, List<String>>>> allFieldMeta, String entityName) {
		for (Map.Entry<String, Map<String, Map<String, List<String>>>> entry : allFieldMeta.entrySet()) {
			if (entry.getKey().contains(entityName) || entry.getKey().equalsIgnoreCase(entityName)) {
				return entry.getValue();
			}
		}
		return null;
	}

}
