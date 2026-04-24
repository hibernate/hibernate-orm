/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.Versioning;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.hibernate.tool.reveng.test.utils.TestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * To be extended by VersioningForJDK50Test for the JPA generation part
 * @author max
 * @author koen
 */
public class TestCase extends TestTemplate {

	private static String[] CREATE_SQLS = {
			"CREATE TABLE WITH_VERSION (ONE INT, TWO INT, VERSION INT, NAME VARCHAR(256), PRIMARY KEY (ONE))",
			"CREATE TABLE NO_VERSION (ONE INT, TWO INT, NAME VARCHAR(256), PRIMARY KEY (TWO))",
			"CREATE TABLE WITH_REAL_TIMESTAMP (ONE INT, TWO INT, DBTIMESTAMP TIMESTAMP, NAME VARCHAR(256), PRIMARY KEY (ONE))",
			"CREATE TABLE WITH_FAKE_TIMESTAMP (ONE INT, TWO INT, DBTIMESTAMP INT, NAME VARCHAR(256), PRIMARY KEY (ONE))"
	};

	private static String[] DROP_SQLS = {
			"DROP TABLE WITH_VERSION",
			"DROP TABLE NO_VERSION",
			"DROP TABLE WITH_REAL_TIMESTAMP",
			"DROP TABLE WITH_FAKE_TIMESTAMP"
	};

	private static String[] HIBERNATE_PROPERTIES = {
			"hibernate.connection.url",
			"hibernate.connection.username",
	};

	private List<ClassDetails> entities = null;
	private MetadataDescriptor metadataDescriptor = null;

	@TempDir
	public File outputFolder = new File("output");

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null);
		entities = ((RevengMetadataDescriptor) metadataDescriptor)
				.getEntityClassDetails();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testVersion() {
		ClassDetails withVersion = findByTableName("WITH_VERSION");
		assertNotNull(withVersion);
		FieldDetails versionField = findVersionField(withVersion);
		assertNotNull(versionField, "WITH_VERSION should have a @Version field");
		assertEquals("version", versionField.getName());

		ClassDetails noVersion = findByTableName("NO_VERSION");
		assertNotNull(noVersion);
		FieldDetails noVersionField = findVersionField(noVersion);
		assertNull(noVersionField, "NO_VERSION should not have a @Version field");
	}

	@Test
	public void testGenerateMappings() throws Exception {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.HBM);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		exporter.start();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		XPath xpath = XPathFactory.newInstance().newXPath();
		// WithVersion.hbm.xml should have a <version> element with name="version"
		Document doc = db.parse(new File(outputFolder, "WithVersion.hbm.xml"));
		NodeList versionNodes = (NodeList) xpath.evaluate(
				"//version", doc, XPathConstants.NODESET);
		assertEquals(1, versionNodes.getLength());
		assertEquals("version", versionNodes.item(0).getAttributes()
				.getNamedItem("name").getNodeValue());
		// NoVersion.hbm.xml should have no <version> or <timestamp> elements
		doc = db.parse(new File(outputFolder, "NoVersion.hbm.xml"));
		versionNodes = (NodeList) xpath.evaluate(
				"//version", doc, XPathConstants.NODESET);
		assertEquals(0, versionNodes.getLength());
		NodeList timestampNodes = (NodeList) xpath.evaluate(
				"//timestamp", doc, XPathConstants.NODESET);
		assertEquals(0, timestampNodes.getLength());
		// WithRealTimestamp.hbm.xml should have a <timestamp> element
		doc = db.parse(new File(outputFolder, "WithRealTimestamp.hbm.xml"));
		timestampNodes = (NodeList) xpath.evaluate(
				"//timestamp", doc, XPathConstants.NODESET);
		assertEquals(1, timestampNodes.getLength());
		// WithFakeTimestamp.hbm.xml should have a <version> element with type="integer"
		doc = db.parse(new File(outputFolder, "WithFakeTimestamp.hbm.xml"));
		versionNodes = (NodeList) xpath.evaluate(
				"//version", doc, XPathConstants.NODESET);
		assertEquals(1, versionNodes.getLength());
		assertEquals("java.lang.Integer", versionNodes.item(0).getAttributes()
				.getNamedItem("type").getNodeValue());
	}

	private ClassDetails findByTableName(String tableName) {
		for (ClassDetails cd : entities) {
			Table tableAnn = cd.getDirectAnnotationUsage(Table.class);
			if (tableAnn != null) {
				String name = tableAnn.name().replace("`", "");
				if (tableName.equals(name) || tableName.equalsIgnoreCase(name)) {
					return cd;
				}
			}
		}
		return null;
	}

	private FieldDetails findVersionField(ClassDetails classDetails) {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.getDirectAnnotationUsage(Version.class) != null) {
				return field;
			}
		}
		return null;
	}

}
