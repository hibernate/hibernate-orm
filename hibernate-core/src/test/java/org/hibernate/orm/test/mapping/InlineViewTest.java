/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import org.hibernate.testing.util.MappingTableHelper;

import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.InlineView;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.relational.naming.spi.LogicalName;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Inline views retain mapping identity and keys without becoming exportable objects.
///
/// @author Steve Ebersole
class InlineViewTest {
	private static final org.hibernate.relational.naming.spi.PhysicalName.Factory COLUMN_NAMES =
			new org.hibernate.relational.naming.spi.PhysicalName.Factory(
					(text, quoted) -> quoted ? text : text.toUpperCase( java.util.Locale.ROOT ) );

	@Test
	void mappingKeysAndQuerySurviveSerialization() {
		final var name = new LogicalName( "Report", false, false );
		final var view = new InlineView( "orm", name, "select id from records" );
		final var column = new Column( MappingTableHelper.columnName( "id", COLUMN_NAMES ) );
		view.addColumn( column );
		final var key = new PrimaryKey( view );
		key.addColumn( column );
		view.setPrimaryKey( key );

		assertThrows( org.hibernate.MappingException.class, key::getExportIdentifier );
		assertTrue( view.isSubselect() );
		assertFalse( view.isPhysicalTable() );
		assertFalse( view.isView() );
		assertFalse( org.hibernate.boot.model.relational.Exportable.class.isInstance( view ) );
		assertEquals( "( select id from records )", view.getTableExpression( null ) );
		assertNotEquals( view, new InlineView( "orm", name, view.getSubselect() ) );

		final var restored = (InlineView) SerializationHelper.clone( view );
		assertEquals( name, restored.getLogicalName() );
		assertEquals( view.getSubselect(), restored.getSubselect() );
		assertFalse( org.hibernate.boot.model.relational.ContributableDatabaseObject.class.isInstance( restored ) );
		assertSame( restored, restored.getPrimaryKey().getTable() );
		assertSame( restored.getColumn( 1 ), restored.getPrimaryKey().getColumn( 0 ) );
	}

	@Test
	void mappingKeysDoNotBecomeDatabaseConstraints() {
		final var name = new org.hibernate.relational.naming.spi.PhysicalName.Factory( (text, quoted) -> text );
		final var table = new org.hibernate.mapping.PhysicalTable( "orm",
				new org.hibernate.relational.naming.spi.QualifiedPhysicalName( null, null, name.create( "records", false ) ), false );
		final var view = new org.hibernate.mapping.DatabaseView( "orm",
				new org.hibernate.relational.naming.spi.QualifiedPhysicalName( null, null, name.create( "report", false ) ), "select id from records" );
		final var key = new org.hibernate.mapping.ForeignKey( table );
		key.setReferencedTable( view );
		assertInstanceOf( org.hibernate.boot.model.relational.ContributableDatabaseObject.class, table );
		assertInstanceOf( org.hibernate.boot.model.relational.ContributableDatabaseObject.class, view );
		assertFalse( key.isPhysicalConstraint() );
		final var inverse = new org.hibernate.mapping.ForeignKey( view );
		inverse.setReferencedTable( table );
		assertFalse( inverse.isPhysicalConstraint() );
		assertThrows( NoSuchMethodException.class, () -> InlineView.class.getMethod( "getIndexes" ) );
		assertThrows( NoSuchMethodException.class, () -> InlineView.class.getMethod( "setOptions", String.class ) );
		assertThrows( NoSuchMethodException.class, () -> org.hibernate.mapping.Table.class.getMethod( "getNameIdentifier" ) );
	}

	@Test
	void requiresLogicalIdentityAndQuery() {
		final var name = new LogicalName( "Report", false, false );
		assertThrows( NullPointerException.class, () -> new InlineView( "orm", null, "select 1" ) );
		assertThrows( IllegalArgumentException.class, () -> new InlineView( "orm", name, " " ) );
	}
}
