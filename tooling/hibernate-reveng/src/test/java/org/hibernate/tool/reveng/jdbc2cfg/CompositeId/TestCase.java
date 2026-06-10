/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.CompositeId;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author max
 * @author koen
 */
@SuppressWarnings("DuplicatedCode")
public class TestCase {

	private MetadataDescriptor metadataDescriptor = null;
	private RevengStrategy reverseEngineeringStrategy = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		reverseEngineeringStrategy = new DefaultStrategy();
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(reverseEngineeringStrategy, null);
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
				JdbcUtil.toIdentifier(this, "LINE_ITEM") );
		assertNotNull(table);
		ForeignKey foreignKey = HibernateUtil.getForeignKey(
				table,
				JdbcUtil.toIdentifier(this, "TO_CUSTOMER_ORDER") );
		assertNotNull(foreignKey);
		assertEquals(
				reverseEngineeringStrategy.tableToClassName(
						TableIdentifier.create(
								null,
								null,
								JdbcUtil.toIdentifier(this, "CUSTOMER_ORDER"))),
				foreignKey.getReferencedEntityName() );
		assertEquals(
				JdbcUtil.toIdentifier(this, "LINE_ITEM"),
				foreignKey.getTable().getName() );
		assertEquals(2,foreignKey.getColumnSpan() );
		assertEquals("CUSTOMER_ID_REF", foreignKey.getColumn(0).getName());
		assertEquals("ORDER_NUMBER", foreignKey.getColumn(1).getName());
		Table tab = HibernateUtil.getTable(
				metadata,
				JdbcUtil.toIdentifier(this, "CUSTOMER_ORDER"));
		assertEquals("CUSTOMER_ID", tab.getPrimaryKey().getColumn(0).getName());
		assertEquals("ORDER_NUMBER", tab.getPrimaryKey().getColumn(1).getName());
		PersistentClass lineMapping = metadata.getEntityBinding(
				reverseEngineeringStrategy.tableToClassName(
						TableIdentifier.create(
								null,
								null,
								JdbcUtil.toIdentifier(this, "LINE_ITEM"))));
		assertEquals(4,lineMapping.getIdentifier().getColumnSpan() );
		Iterator<Column> columnIterator = lineMapping.getIdentifier().getColumns().iterator();
		assertEquals("CUSTOMER_ID_REF", columnIterator.next().getName());
		assertEquals("EXTRA_PROD_ID", columnIterator.next().getName());
		assertEquals("ORDER_NUMBER", columnIterator.next().getName());
	}

	@Test
	public void testPossibleKeyManyToOne() {
		PersistentClass product = metadataDescriptor.createMetadata().getEntityBinding(
				reverseEngineeringStrategy.tableToClassName(
						TableIdentifier.create(
								null,
								null,
								JdbcUtil.toIdentifier(this, "CUSTOMER_ORDER"))));
		Property identifierProperty = product.getIdentifierProperty();
		assertInstanceOf(Component.class, identifierProperty.getValue());
		Component cmpid = (Component) identifierProperty.getValue();
		assertEquals(2, cmpid.getPropertySpan() );
		Iterator<?> iter = cmpid.getProperties().iterator();
		Property id = (Property) iter.next();
		Property extraId = (Property) iter.next();
		assertEquals(
				reverseEngineeringStrategy.columnToPropertyName(
						null,
						"CUSTOMER_ID"),
				id.getName() );
		assertEquals(
				reverseEngineeringStrategy.columnToPropertyName(
						null,
						"ORDER_NUMBER"),
				extraId.getName() );
		assertFalse(id.getValue() instanceof ManyToOne);
		assertFalse(extraId.getValue() instanceof ManyToOne);
	}

	@Test
	public void testKeyProperty() {
		PersistentClass product = metadataDescriptor.createMetadata().getEntityBinding(
				reverseEngineeringStrategy.tableToClassName(
						TableIdentifier.create(
								null,
								null,
								JdbcUtil.toIdentifier(this, "PRODUCT"))));
		Property identifierProperty = product.getIdentifierProperty();
		assertInstanceOf(Component.class, identifierProperty.getValue());
		Component cmpid = (Component) identifierProperty.getValue();
		assertEquals(2, cmpid.getPropertySpan() );
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
						"PRODUCT_ID"),
				id.getName() );
		assertEquals(
				reverseEngineeringStrategy.columnToPropertyName(
						null,
						"EXTRA_ID"),
				extraId.getName() );
		assertFalse(id.getValue() instanceof ManyToOne);
		assertFalse(extraId.getValue() instanceof ManyToOne);
	}

}
