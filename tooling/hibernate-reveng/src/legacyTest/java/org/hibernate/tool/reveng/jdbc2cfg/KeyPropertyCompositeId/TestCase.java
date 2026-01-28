/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.KeyPropertyCompositeId;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.export.hbm.HbmExporter;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author max
 * @author koen
 */
@SuppressWarnings("DuplicatedCode")
public class TestCase {

	@TempDir
	public File outputDir = new File("output");

	private MetadataDescriptor metadataDescriptor = null;
	private RevengStrategy reverseEngineeringStrategy = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		reverseEngineeringStrategy = new DefaultStrategy();
		Properties properties = new Properties();
		properties.put(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, false);
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(reverseEngineeringStrategy, properties);
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testMultiColumnForeignKeys() {
		Metadata metadata = metadataDescriptor.createMetadata();
		Table table = HibernateUtil.getTable(
				metadata,
				JdbcUtil.toIdentifier(this, "LINE_ITEM"));
		assertNotNull(table);
		ForeignKey foreignKey = HibernateUtil.getForeignKey(table, JdbcUtil.toIdentifier(this, "TO_CUSTOMER_ORDER"));
		assertNotNull(foreignKey);
		assertEquals(
				reverseEngineeringStrategy.tableToClassName(
						TableIdentifier.create(null, null, JdbcUtil.toIdentifier(this, "CUSTOMER_ORDER"))),
				foreignKey.getReferencedEntityName());
		assertEquals(JdbcUtil.toIdentifier(this, "LINE_ITEM"), foreignKey.getTable().getName());
		assertEquals(2, foreignKey.getColumnSpan());
		assertEquals("CUSTOMER_ID_REF", foreignKey.getColumn(0).getName());
		assertEquals("ORDER_NUMBER", foreignKey.getColumn(1).getName());
		Table tab = HibernateUtil.getTable(
				metadata,
				JdbcUtil.toIdentifier(this, "CUSTOMER_ORDER"));
		assertEquals("CUSTOMER_ID", tab.getPrimaryKey().getColumn(0).getName());
		assertEquals("ORDER_NUMBER", tab.getPrimaryKey().getColumn(1).getName());
		PersistentClass lineMapping = metadata.getEntityBinding(
				reverseEngineeringStrategy
					.tableToClassName(TableIdentifier.create(null, null, JdbcUtil.toIdentifier(this, "LINE_ITEM"))));
		assertEquals(4, lineMapping.getIdentifier().getColumnSpan());
		Iterator<Column> columnIterator = lineMapping.getIdentifier().getColumns().iterator();
		assertEquals("CUSTOMER_ID_REF", columnIterator.next().getName());
		assertEquals("ORDER_NUMBER", columnIterator.next().getName());
	}

	@Test
	public void testPossibleKeyManyToOne() {
		PersistentClass product = metadataDescriptor.createMetadata().getEntityBinding(
				reverseEngineeringStrategy
					.tableToClassName(TableIdentifier.create(null, null, JdbcUtil.toIdentifier(this, "CUSTOMER_ORDER"))));
		Property identifierProperty = product.getIdentifierProperty();
		assertInstanceOf(Component.class, identifierProperty.getValue());
		Component cmpid = (Component) identifierProperty.getValue();
		assertEquals(2, cmpid.getPropertySpan());
		Iterator<?> iter = cmpid.getProperties().iterator();
		Property id = (Property) iter.next();
		Property extraId = (Property) iter.next();
		assertEquals(
				reverseEngineeringStrategy.columnToPropertyName(
						null,
						"customer"),
				id.getName());
		assertEquals(
				reverseEngineeringStrategy.columnToPropertyName(
						null,
						"orderNumber"),
				extraId.getName());
		assertInstanceOf(ManyToOne.class, id.getValue());
		assertFalse(extraId.getValue() instanceof ManyToOne);
	}

	@Test
	public void testKeyProperty() {
		PersistentClass product = metadataDescriptor.createMetadata().getEntityBinding(
				reverseEngineeringStrategy
					.tableToClassName(TableIdentifier.create(null, null, JdbcUtil.toIdentifier(this, "PRODUCT"))));
		Property identifierProperty = product.getIdentifierProperty();
		assertInstanceOf(Component.class, identifierProperty.getValue());
		Component cmpid = (Component) identifierProperty.getValue();
		assertEquals(2, cmpid.getPropertySpan());
		Iterator<?> iter = cmpid.getProperties().iterator();
		Property id = (Property)iter.next();
		Property extraId = (Property)iter.next();
		if ("extraId".equals(id.getName())) {
			Property temp = id;
			id = extraId;
			extraId = temp;
		}
		assertEquals(
				reverseEngineeringStrategy.columnToPropertyName(
						null,
						"productId"),
				id.getName());
		assertEquals(
				reverseEngineeringStrategy.columnToPropertyName(
						null,
						"extraId"),
				extraId.getName());
		assertFalse(id.getValue() instanceof ManyToOne);
		assertFalse(extraId.getValue() instanceof ManyToOne);
	}

	@Test
	public void testGeneration() throws Exception {
		Exporter exporter = new HbmExporter();
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		Exporter javaExp = ExporterFactory.createExporter(ExporterType.JAVA);
		javaExp.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
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
		session.createQuery("from LineItem", null).getResultList();
		List<?> list = session.createQuery("from Product", null).getResultList();
		assertEquals(2, list.size());
		list = session
				.createQuery("select li.id.customerOrder.id from LineItem as li", null)
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

}
