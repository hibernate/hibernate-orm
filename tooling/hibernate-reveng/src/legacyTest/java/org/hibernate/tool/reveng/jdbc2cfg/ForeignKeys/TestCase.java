/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.ForeignKeys;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private Metadata metadata = null;
	private RevengStrategy reverseEngineeringStrategy = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		reverseEngineeringStrategy = new DefaultStrategy();
		metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(reverseEngineeringStrategy, null)
				.createMetadata();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testMultiRefs() {
		Table table = HibernateUtil.getTable(
				metadata,
				JdbcUtil.toIdentifier(this, "CONNECTION") );
		ForeignKey foreignKey = HibernateUtil.getForeignKey(
				table,
				JdbcUtil.toIdentifier(this, "CON2MASTER") );
		assertNotNull(foreignKey);
		assertEquals(
				reverseEngineeringStrategy.tableToClassName(
						TableIdentifier.create(null, null, "MASTER")),
				foreignKey.getReferencedEntityName() );
		assertEquals(
				JdbcUtil.toIdentifier(this, "CONNECTION"),
				foreignKey.getTable().getName() );
		assertEquals(
				HibernateUtil.getTable(
						metadata,
						JdbcUtil.toIdentifier(this, "MASTER") ),
				foreignKey.getReferencedTable() );
		assertNotNull(
				HibernateUtil.getForeignKey(
						table,
						JdbcUtil.toIdentifier(this, "CHILDREF1") ) );
		assertNotNull(
				HibernateUtil.getForeignKey(
						table,
						JdbcUtil.toIdentifier(this, "CHILDREF2") ) );
		assertNull(
				HibernateUtil.getForeignKey(
						table,
						JdbcUtil.toIdentifier(this, "DUMMY") ) );
		JUnitUtil.assertIteratorContainsExactly(null, table.getForeignKeyCollection().iterator(), 3);
	}

	@Test
	public void testMasterChild() {
		assertNotNull(HibernateUtil.getTable(
				metadata,
				JdbcUtil.toIdentifier(this, "MASTER")));
		Table child = HibernateUtil.getTable(
				metadata,
				JdbcUtil.toIdentifier(this, "CHILD") );
		Iterator<?> iterator = child.getForeignKeyCollection().iterator();
		ForeignKey fk = (ForeignKey) iterator.next();
		assertFalse(iterator.hasNext(), "should only be one fk" );
		assertEquals(1, fk.getColumnSpan() );
		assertSame(
				fk.getColumn(0),
				child.getColumn(
						new Column(JdbcUtil.toIdentifier(this, "MASTERREF"))));
	}

	@Test
	public void testExport() {
		SchemaExport schemaExport = new SchemaExport();
		final EnumSet<TargetType> targetTypes = EnumSet.noneOf( TargetType.class );
		targetTypes.add( TargetType.STDOUT );
		schemaExport.create(targetTypes, metadata);
	}

}
