/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.KeyPropertyCompositeId;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.reveng.internal.metadata.NativeMetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.entity.EntityExporter;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.api.metadata.MetadataConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.reveng.internal.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.test.utils.JavaUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
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
			}
		else {
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
	public void testGeneration() throws Exception {
		Properties freshProperties = new Properties();
		freshProperties.put(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, false);
		MetadataDescriptor freshDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(reverseEngineeringStrategy, freshProperties);
		EntityExporter.create(freshDescriptor, true).exportAll(outputDir);
		List<String> paths = new java.util.ArrayList<>();
		paths.add(JavaUtil.resolvePathToJarFileFor(jakarta.persistence.Persistence.class));
		paths.add(JavaUtil.resolvePathToJarFileFor(org.hibernate.Version.class));
		JavaUtil.compile(outputDir, paths);
		URL[] urls = new URL[] { outputDir.toURI().toURL() };
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader ucl = new URLClassLoader(urls, oldLoader);
		try {
			Thread.currentThread().setContextClassLoader(ucl);
			org.hibernate.boot.registry.StandardServiceRegistryBuilder builder =
					new org.hibernate.boot.registry.StandardServiceRegistryBuilder();
			org.hibernate.service.ServiceRegistry serviceRegistry = builder.build();
			NativeMetadataDescriptor mds = new NativeMetadataDescriptor(null, null, null);
			for (File f : outputDir.listFiles()) {
				if (f.getName().endsWith(".class")) {
					String className = f.getName().replace(".class", "");
					HibernateUtil.addAnnotatedClass(mds, ucl.loadClass(className));
				}
			}
			new org.hibernate.tool.hbm2ddl.SchemaValidator()
					.validate(mds.createMetadata(), serviceRegistry);
			Class<?> productIdClass = ucl.loadClass("ProductId");
			Constructor<?> productIdConstructor = productIdClass.getConstructor();
			Object object = productIdConstructor.newInstance();
			int hash = -1;
			try {
				hash = object.hashCode();
			}
		catch (Throwable t) {
				fail("Hashcode on new instance should not fail " + t);
			}
			assertNotEquals(hash, System.identityHashCode(object), "hashcode should be different from system");
		}
		finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
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
