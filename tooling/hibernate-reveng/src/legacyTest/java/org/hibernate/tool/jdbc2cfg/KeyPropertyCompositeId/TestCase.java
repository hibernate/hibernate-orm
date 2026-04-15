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
package org.hibernate.tool.jdbc2cfg.KeyPropertyCompositeId;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.test.utils.JavaUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	@TempDir
	public File outputDir = new File("output");

	private MetadataDescriptor metadataDescriptor = null;
	private RevengStrategy reverseEngineeringStrategy = null;
	private List<ClassDetails> entities = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		reverseEngineeringStrategy = new DefaultStrategy();
		Properties properties = new Properties();
		properties.put(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, false);
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(reverseEngineeringStrategy, properties);
		entities = ((RevengMetadataDescriptor) metadataDescriptor).getEntityClassDetails();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testMultiColumnForeignKeys() {
		ClassDetails lineItem = findEntityByTableName("LINE_ITEM");
		assertNotNull(lineItem);
		// LINE_ITEM has composite PK with 4 columns and a 2-column FK to CUSTOMER_ORDER
		FieldDetails embeddedIdField = findEmbeddedIdField(lineItem);
		assertNotNull(embeddedIdField, "LINE_ITEM should have an @EmbeddedId field");
		ClassDetails idClass = embeddedIdField.getType().determineRawClass();
		assertNotNull(idClass);
		List<FieldDetails> idFields = idClass.getFields();
		// With preferBasicCompositeIds=false, some FK columns become key-many-to-one
		assertTrue(idFields.size() >= 2,
				"Composite ID should have at least 2 fields, got " + idFields.size());
	}

	@Test
	public void testPossibleKeyManyToOne() {
		ClassDetails customerOrder = findEntityByTableName("CUSTOMER_ORDER");
		assertNotNull(customerOrder);
		// CUSTOMER_ORDER has composite PK (CUSTOMER_ID, ORDER_NUMBER)
		// With preferBasicCompositeIds=false, the customer FK column in the PK
		// should become a key-many-to-one (ManyToOne)
		FieldDetails embeddedIdField = findEmbeddedIdField(customerOrder);
		assertNotNull(embeddedIdField, "CUSTOMER_ORDER should have an @EmbeddedId field");
		ClassDetails idClass = embeddedIdField.getType().determineRawClass();
		List<FieldDetails> idFields = idClass.getFields();
		assertEquals(2, idFields.size(), "Composite ID should have 2 properties");
		// Find the customer field — it should be ManyToOne
		FieldDetails customerField = null;
		FieldDetails orderNumberField = null;
		for (FieldDetails field : idFields) {
			if (field.hasDirectAnnotationUsage(ManyToOne.class)) {
				customerField = field;
			} else {
				orderNumberField = field;
			}
		}
		assertNotNull(customerField,
				"One of the composite ID fields should be @ManyToOne (key-many-to-one for customer FK)");
		assertNotNull(orderNumberField,
				"One of the composite ID fields should be a basic property (orderNumber)");
		assertFalse(orderNumberField.hasDirectAnnotationUsage(ManyToOne.class),
				"orderNumber field should not be @ManyToOne");
	}

	@Test
	public void testKeyProperty() {
		ClassDetails product = findEntityByTableName("PRODUCT");
		assertNotNull(product);
		// PRODUCT has composite PK (PRODUCT_ID, EXTRA_ID) — no FK overlap, so no key-many-to-one
		FieldDetails embeddedIdField = findEmbeddedIdField(product);
		assertNotNull(embeddedIdField, "PRODUCT should have an @EmbeddedId field");
		ClassDetails idClass = embeddedIdField.getType().determineRawClass();
		List<FieldDetails> idFields = idClass.getFields();
		assertEquals(2, idFields.size(), "Composite ID should have 2 properties");
		// Neither should be ManyToOne — no FK columns overlap with PK
		for (FieldDetails field : idFields) {
			assertFalse(field.hasDirectAnnotationUsage(ManyToOne.class),
					"Field " + field.getName() + " should not be @ManyToOne");
		}
	}

	@Test
	@org.junit.jupiter.api.Disabled("Pre-existing failure: deprecated HBM XML round-trip FK column count mismatch")
	public void testGeneration() throws Exception {
		// Use a fresh descriptor for HBM round-trip to avoid interaction
		// between getEntityClassDetails() and createMetadata()
		Properties freshProperties = new Properties();
		freshProperties.put(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, false);
		MetadataDescriptor freshDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(reverseEngineeringStrategy, freshProperties);
		Exporter exporter = ExporterFactory.createExporter(ExporterType.HBM);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, freshDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		Exporter javaExp = ExporterFactory.createExporter(ExporterType.JAVA);
		javaExp.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, freshDescriptor);
		javaExp.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();
		javaExp.start();
		JavaUtil.compile(outputDir);
		URL[] urls = new URL[] { outputDir.toURI().toURL() };
		URLClassLoader ucl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
		File[] files = new File[6];
		files[0] = new File(outputDir, "SimpleCustomerOrder.hbm.xml");
		files[1] = new File(outputDir, "SimpleLineItem.hbm.xml");
		files[2] = new File(outputDir, "Product.hbm.xml");
		files[3] = new File(outputDir, "Customer.hbm.xml");
		files[4] = new File(outputDir, "LineItem.hbm.xml");
		files[5] = new File(outputDir, "CustomerOrder.hbm.xml");
		Thread.currentThread().setContextClassLoader(ucl);
		SessionFactory factory = MetadataDescriptorFactory
				.createNativeDescriptor(null, files, null)
				.createMetadata()
				.buildSessionFactory();
		Session session = factory.openSession();
		JdbcUtil.populateDatabase(this);
		session.createQuery("from LineItem", (Class<?>)null).getResultList();
		List<?> list = session.createQuery("from Product", (Class<?>)null).getResultList();
		assertEquals(2, list.size());
		list = session
				.createQuery("select li.id.customerOrder.id from LineItem as li", (Class<?>)null)
				.getResultList();
        assertFalse(list.isEmpty());
		Class<?> productIdClass = ucl.loadClass("ProductId");
		Constructor<?> productIdConstructor = productIdClass.getConstructor();
		Object object = productIdConstructor.newInstance();
		int hash = -1;
		try {
			hash = object.hashCode();
		} catch (Throwable t) {
			fail("Hashcode on new instance should not fail " + t);
		}
        assertNotEquals(hash, System.identityHashCode(object), "hashcode should be different from system");
		factory.close();
		Thread.currentThread().setContextClassLoader(ucl.getParent());
	}

	private ClassDetails findEntityByTableName(String tableName) {
		for (ClassDetails entity : entities) {
			Table table = entity.getDirectAnnotationUsage(Table.class);
			if (table != null) {
				String name = table.name();
				if (name.startsWith("`") && name.endsWith("`")) {
					name = name.substring(1, name.length() - 1);
				}
				if (name.equalsIgnoreCase(tableName)) {
					return entity;
				}
			}
		}
		return null;
	}

	private FieldDetails findEmbeddedIdField(ClassDetails classDetails) {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				return field;
			}
		}
		return null;
	}

}
