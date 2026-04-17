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
package org.hibernate.tool.jdbc2cfg.CompositeId;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.internal.metadata.NativeMetadataDescriptor;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.internal.exporter.entity.EntityExporter;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.test.utils.HibernateUtil;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private MetadataDescriptor metadataDescriptor = null;
	private List<ClassDetails> entities = null;

	@TempDir
	public File outputDir = new File("output");

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(new DefaultStrategy(), null);
		entities = ((RevengMetadataDescriptor) metadataDescriptor)
				.getEntityClassDetails();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testMultiColumnForeignKeys() {
		ClassDetails lineItem = findEntity("LineItem");
		assertNotNull(lineItem, "LineItem entity should exist");
		// LineItem should have an @EmbeddedId (composite PK of 4 columns)
		FieldDetails embeddedIdField = findEmbeddedIdField(lineItem);
		assertNotNull(embeddedIdField, "LineItem should have @EmbeddedId");
		// Find the embeddable and verify it has 4 fields
		String embeddableTypeName = embeddedIdField.getType().getName();
		ClassDetails embeddable = findEntity(embeddableTypeName);
		assertNotNull(embeddable, "Embeddable " + embeddableTypeName + " should exist");
		assertEquals(4, embeddable.getFields().size(),
				"LineItem composite ID should have 4 fields");
		// LineItem should have a @ManyToOne for the FK to CustomerOrder
		FieldDetails customerOrderRef = lineItem.getFields().stream()
				.filter(f -> f.hasDirectAnnotationUsage(ManyToOne.class))
				.filter(f -> f.getType().getName().equals("CustomerOrder"))
				.findFirst().orElse(null);
		assertNotNull(customerOrderRef,
				"LineItem should have @ManyToOne referencing CustomerOrder");
		// The FK has 2 columns — verify @JoinColumns
		JoinColumns joinColumns = customerOrderRef
				.getDirectAnnotationUsage(JoinColumns.class);
		assertNotNull(joinColumns,
				"Multi-column FK should use @JoinColumns");
		assertEquals(2, joinColumns.value().length,
				"FK TO_CUSTOMER_ORDER should have 2 join columns");
	}

	@Test
	public void testPossibleKeyManyToOne() {
		ClassDetails customerOrder = findEntity("CustomerOrder");
		assertNotNull(customerOrder, "CustomerOrder entity should exist");
		FieldDetails embeddedIdField = findEmbeddedIdField(customerOrder);
		assertNotNull(embeddedIdField,
				"CustomerOrder should have @EmbeddedId");
		String embeddableTypeName = embeddedIdField.getType().getName();
		ClassDetails embeddable = findEntity(embeddableTypeName);
		assertNotNull(embeddable,
				"Embeddable " + embeddableTypeName + " should exist");
		assertEquals(2, embeddable.getFields().size(),
				"CustomerOrder composite ID should have 2 fields");
		// With preferBasicCompositeIds=true, neither PK field should
		// be a @ManyToOne (key-many-to-one)
		for (FieldDetails field : embeddable.getFields()) {
			assertFalse(field.hasDirectAnnotationUsage(ManyToOne.class),
					"PK field " + field.getName()
					+ " should not be @ManyToOne");
		}
	}

	@Test
	public void testKeyProperty() {
		ClassDetails product = findEntity("Product");
		assertNotNull(product, "Product entity should exist");
		FieldDetails embeddedIdField = findEmbeddedIdField(product);
		assertNotNull(embeddedIdField, "Product should have @EmbeddedId");
		String embeddableTypeName = embeddedIdField.getType().getName();
		ClassDetails embeddable = findEntity(embeddableTypeName);
		assertNotNull(embeddable,
				"Embeddable " + embeddableTypeName + " should exist");
		assertEquals(2, embeddable.getFields().size(),
				"Product composite ID should have 2 fields");
		// Neither PK field should be a @ManyToOne
		for (FieldDetails field : embeddable.getFields()) {
			assertFalse(field.hasDirectAnnotationUsage(ManyToOne.class),
					"PK field " + field.getName()
					+ " should not be @ManyToOne");
		}
	}
     
    @Test
    public void testGeneration() throws Exception {
        EntityExporter.create(metadataDescriptor, true).exportAll(outputDir);
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
            Constructor<?> productIdClassConstructor = productIdClass.getConstructor();
            Object object = productIdClassConstructor.newInstance();
            int hash = -1;
            try {
                hash = object.hashCode();
            } catch (Throwable t) {
                fail("Hashcode on new instance should not fail " + t);
            }
            assertNotEquals(hash, System.identityHashCode(object), "hashcode should be different from system");
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

	private ClassDetails findEntity(String name) {
		return entities.stream()
				.filter(cd -> cd.getName().equals(name)
						|| cd.getName().equalsIgnoreCase(name))
				.findFirst().orElse(null);
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
