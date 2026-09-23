/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import org.hibernate.testing.util.MappingTableHelper;

import java.util.List;
import java.util.HashSet;

import org.hibernate.MappingException;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.QualifiedColumnName;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.mapping.MappedSuperclassColumnContainer;
import org.hibernate.mapping.PrimaryKey;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Declaration column ownership preserves column behavior without acquiring table semantics.
///
/// @author Steve Ebersole
class ColumnContainerTest {
	private static final org.hibernate.relational.naming.spi.PhysicalName.Factory COLUMN_NAMES =
			new org.hibernate.relational.naming.spi.PhysicalName.Factory(
					(text, quoted) -> quoted ? text : text.toUpperCase( java.util.Locale.ROOT ) );

	@Test
	void declarationColumnsMergeOrderAndSurviveSerialization() {
		final var container = new MappedSuperclassColumnContainer( "Base#mapped-superclass" );
		final var first = new Column( MappingTableHelper.columnName( "first", COLUMN_NAMES ) );
		final var second = new Column( MappingTableHelper.columnName( "second", COLUMN_NAMES ) );
		container.addColumn( first );
		container.addColumn( second );
		final var duplicate = new Column( MappingTableHelper.columnName( "FIRST", COLUMN_NAMES ) );
		duplicate.setNullable( false );
		container.addColumn( duplicate );
		assertEquals( 2, container.getColumnSpan() );
		assertSame( first, container.getColumn( duplicate ) );
		assertFalse( first.isNullable() );
		container.reorderColumns( List.of( second, first ) );
		assertSame( second, container.getColumn( 1 ) );
		container.renameColumn( second, COLUMN_NAMES.create( "renamed", false ) );
		assertSame( second, container.getColumn( MappingTableHelper.columnName( "renamed", COLUMN_NAMES ) ) );
		assertNull( container.getColumn( MappingTableHelper.columnName( "second", COLUMN_NAMES ) ) );
		assertThrows( MappingException.class, container::requireTable );
		assertNotEquals( container, new MappedSuperclassColumnContainer( container.getDiagnosticLabel() ) );

		final var restored = (MappedSuperclassColumnContainer) SerializationHelper.clone( container );
		final var lifecycle = new org.hibernate.mapping.ColumnNameLifecycle();
		lifecycle.addContainer( restored );
		lifecycle.restore( COLUMN_NAMES );
		assertEquals( container.getDiagnosticLabel(), restored.getDiagnosticLabel() );
		assertEquals( 2, restored.getColumnSpan() );
		assertFalse( restored.getColumn( MappingTableHelper.columnName( "first", COLUMN_NAMES ) ).isNullable() );
		assertThrows( MappingException.class, restored::requireTable );
	}

	@Test
	void duplicateColumnsUseDeclarationOwnership() {
		try ( var registry = ServiceRegistryUtil.serviceRegistry() ) {
			final var context = new MetadataBuildingContextTestingImpl( registry );
			final var database = context.getMetadataCollector().getDatabase();
			final var owner = new MappedSuperclassColumnContainer( "Base#mapped-superclass" );
			final var first = new BasicValue( context, owner );
			first.addColumn( new Column( MappingTableHelper.columnName( "shared", COLUMN_NAMES ) ) );
			final var duplicate = new BasicValue( context, owner );
			duplicate.addColumn( new Column( MappingTableHelper.columnName( "shared", COLUMN_NAMES ) ) );
			final var other = new BasicValue( context,
					new MappedSuperclassColumnContainer( "Base#mapped-superclass" ) );
			other.addColumn( new Column( MappingTableHelper.columnName( "shared", COLUMN_NAMES ) ) );
			final var columns = new HashSet<QualifiedColumnName>();
			first.checkColumnDuplication( columns, "first", database );
			other.checkColumnDuplication( columns, "other", database );
			assertEquals( 2, columns.size() );
			final var error = assertThrows( MappingException.class,
					() -> duplicate.checkColumnDuplication( columns, "duplicate", database ) );
			assertTrue( error.getMessage().contains( "is duplicated in mapping" ) );
		}
	}

	@Test
	void relationalPrimaryKeyStillForcesColumnNonNull() {
		final var table = MappingTableHelper.table( "orm", "entity_table", new org.hibernate.relational.naming.spi.PhysicalName.Factory( (text, quoted) -> text ) );
		final var key = new PrimaryKey( table );
		key.addColumn( new Column( MappingTableHelper.columnName( "id", COLUMN_NAMES ) ) );
		table.setPrimaryKey( key );
		final var column = new Column( MappingTableHelper.columnName( "id", COLUMN_NAMES ) );
		table.addColumn( column );
		assertFalse( column.isNullable() );
		assertSame( table, table.requireTable() );
	}
}
