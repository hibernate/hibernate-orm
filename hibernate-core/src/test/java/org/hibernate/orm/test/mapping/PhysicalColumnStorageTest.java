/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import java.util.List;
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ColumnNameLifecycle;
import org.hibernate.mapping.MappedSuperclassColumnContainer;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.util.MappingTableHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Physical column identity survives controlled mutation and attachment to a new environment.
///
/// @author Steve Ebersole
class PhysicalColumnStorageTest {
	private static final PhysicalName.Factory EXACT = new PhysicalName.Factory( (text, quoted) -> text );
	private static final PhysicalName.Factory FOLDED = new PhysicalName.Factory(
			(text, quoted) -> quoted ? text : text.toUpperCase( Locale.ROOT ) );

	@Test
	void equalityAndCopiesUseFinalizedNames() {
		final var plain = new Column( FOLDED.create( "name", false ) );
		final var quoted = new Column( FOLDED.create( "NAME", true ) );
		assertEquals( plain, quoted );
		assertEquals( quoted, plain );
		assertEquals( plain.hashCode(), quoted.hashCode() );
		assertNotEquals( plain, new Column( FOLDED.create( "name", true ) ) );
		assertSame( plain.getPhysicalName(), plain.clone().getPhysicalName() );
		assertSame( plain.getPhysicalName(), new org.hibernate.mapping.AggregateColumn( plain, null ).getPhysicalName() );
		final var destination = new Column( FOLDED.create( "destination", false ) );
		destination.copy( plain );
		assertEquals( "destination", destination.getName() );
		assertThrows( NullPointerException.class, () -> new Column( null ) );
	}

	@Test
	void observedNamesAreNotFoldedAsMappingInputs() {
		final var owner = new MappedSuperclassColumnContainer( "Base" );
		final var unquoted = new Column( FOLDED.create( "name", false ) );
		final var quoted = new Column( FOLDED.create( "name", true ) );
		owner.addColumn( unquoted );
		owner.addColumn( quoted );
		assertSame( unquoted, owner.getColumnByDatabaseName( "NAME", FOLDED.getComparisonPolicy() ) );
		assertSame( quoted, owner.getColumnByDatabaseName( "name", FOLDED.getComparisonPolicy() ) );
		assertNull( owner.getColumnByDatabaseName( "Name", FOLDED.getComparisonPolicy() ) );
	}

	@Test
	void renameValidatesBeforeChangingIdentityAndPreservesOrder() {
		final var owner = new MappedSuperclassColumnContainer( "Base" );
		final var first = new Column( FOLDED.create( "first", false ) );
		final var second = new Column( FOLDED.create( "second", false ) );
		owner.addColumn( first );
		owner.addColumn( second );
		assertThrows( MappingException.class,
				() -> owner.renameColumn( first, second.getPhysicalName() ) );
		assertEquals( "first", first.getName() );
		assertSame( first, owner.getColumn( FOLDED.create( "FIRST", true ) ) );
		assertThrows( MappingException.class,
				() -> owner.renameColumn( first.clone(), FOLDED.create( "third", false ) ) );
		owner.renameColumn( first, FOLDED.create( "FIRST", true ) );
		assertTrue( first.isQuoted() );
		owner.renameColumn( first, FOLDED.create( "renamed", false ) );
		assertNull( owner.getColumn( FOLDED.create( "first", false ) ) );
		assertSame( first, owner.getColumn( 1 ) );
		assertSame( first, owner.getColumn( FOLDED.create( "renamed", false ) ) );
	}

	@Test
	void coordinatedRenameUpdatesAliasesInheritanceAndIncomingKeys() {
		final var parent = MappingTableHelper.table( "orm", "parent", FOLDED );
		final var child = new org.hibernate.mapping.DenormalizedTable( "orm",
				new org.hibernate.relational.naming.spi.QualifiedPhysicalName( null, null, FOLDED.create( "child", false ) ), false, parent );
		final var source = MappingTableHelper.table( "orm", "source", FOLDED );
		final var target = new Column( FOLDED.create( "target", false ) );
		parent.addColumn( target );
		final var local = new Column( FOLDED.create( "local", false ) );
		source.addColumn( local );
		final var fk = source.createForeignKey( "fk", List.of( local ), "Parent", null, null, List.of( target ) );
		fk.setReferencedTable( parent );
		final var database = org.mockito.Mockito.mock( org.hibernate.boot.model.relational.Database.class );
		final var namespace = org.mockito.Mockito.mock( org.hibernate.boot.model.relational.Namespace.class );
		org.mockito.Mockito.when( namespace.getTables() ).thenReturn( List.of( parent, child, source ) );
		org.mockito.Mockito.when( database.getNamespaces() ).thenReturn( List.of( namespace ) );
		final var correspondences = new org.hibernate.boot.mapping.internal.relational.RelationalModelCorrespondences( database );
		final var logical = new org.hibernate.relational.naming.spi.LogicalName( "sourceName", false, true );
		final var alias = new org.hibernate.relational.naming.spi.LogicalName( "alias", false, false );
		correspondences.columnNames().register( parent, logical, target );
		correspondences.columnNames().register( parent, alias, target );
		final var state = org.mockito.Mockito.mock( org.hibernate.boot.mapping.internal.context.BindingState.class,
				org.mockito.Mockito.CALLS_REAL_METHODS );
		org.mockito.Mockito.when( state.getDatabase() ).thenReturn( database );
		org.mockito.Mockito.when( state.getEntityBindings() ).thenReturn( List.of() );
		org.mockito.Mockito.when( state.getRelationalModelCorrespondences() ).thenReturn( correspondences );
		state.renameColumn( parent, target, FOLDED.create( "renamed", false ) );
		assertSame( target, child.getColumn( FOLDED.create( "renamed", false ) ) );
		assertNull( correspondences.columnNames().findLogicalName( parent, FOLDED.create( "target", false ) ) );
		assertSame( target, correspondences.columnNames().findPhysicalColumn( child, logical ) );
		assertSame( target, correspondences.columnNames().findPhysicalColumn( child, alias ) );
		assertSame( fk, source.createForeignKey( "fk", List.of( local ), "Parent", null, null, List.of( target ) ) );
		final var otherTarget = new Column( FOLDED.create( "collision", false ) );
		source.createForeignKey( "other_fk", List.of( local ), "Parent", null, null, List.of( otherTarget ) );
		assertThrows( MappingException.class,
				() -> state.renameColumn( parent, target, otherTarget.getPhysicalName() ) );
		assertEquals( "renamed", target.getName() );
		assertNotNull( correspondences.columnNames().findLogicalName( parent, target.getPhysicalName() ) );
	}

	@Test
	void collectorCompatibilityLookupsFollowRenamedColumnReferences() {
		try ( var registry = org.hibernate.testing.util.ServiceRegistryUtil.serviceRegistry() ) {
			final var context = new org.hibernate.testing.boot.MetadataBuildingContextTestingImpl( registry );
			final var collector = context.getMetadataCollector();
			final var factory = collector.getDatabase().getJdbcEnvironment().getIdentifierHelper().getPhysicalNameFactory();
			final var table = MappingTableHelper.table( "orm", "records", factory );
			final var column = new Column( factory.create( "old_name", false ) );
			table.addColumn( column );
			collector.addColumnNameBinding( table, "logical", column );
			table.renameColumn( column, factory.create( "new_name", false ) );
			assertEquals( "new_name", collector.getPhysicalColumnName( table, "logical" ) );
			assertEquals( "logical", collector.getLogicalColumnName( table, "new_name" ) );
			assertThrows( MappingException.class, () -> collector.getLogicalColumnName( table, "old_name" ) );
		}
	}

	@Test
	void archiveRebuildsConstraintIndexesWithTheTargetPolicy() {
		final var table = MappingTableHelper.table( "orm", "records", EXACT );
		final var column = new Column( EXACT.create( "code", false ) );
		table.addColumn( column );
		final var key = new UniqueKey( table );
		key.setName( "uk_records" );
		key.addColumn( column, "desc" );
		table.addUniqueKey( key );
		final var index = new Index();
		index.setName( "idx_records" );
		index.setTable( table );
		index.addColumn( column, "asc" );
		table.addIndex( index );
		table.createForeignKey( "fk_records", List.of( column ), "Target", null, null, null );
		var restored = (org.hibernate.mapping.PhysicalTable) SerializationHelper.clone( table );
		for ( int i = 0; i < 2; i++ ) {
			final var lifecycle = new ColumnNameLifecycle();
			lifecycle.addContainer( restored );
			lifecycle.restore( FOLDED );
			final var restoredColumn = restored.getColumn( FOLDED.create( "CODE", true ) );
			assertNotNull( restoredColumn );
			assertEquals( "code", restoredColumn.getName() );
			assertEquals( "desc", restored.getUniqueKeys().get( "uk_records" ).getColumnOrderMap()
					.get( new Column( FOLDED.create( "CODE", true ) ) ) );
			assertEquals( "asc", restored.getIndexes().get( "idx_records" ).getSelectableOrderMap().get( restoredColumn ) );
			assertSame( restored.getForeignKeys().iterator().next(),
					restored.createForeignKey( "fk_records", List.of( restoredColumn ), "Target", null, null, null ) );
			restored = (org.hibernate.mapping.PhysicalTable) SerializationHelper.clone( restored );
		}
	}

	@Test
	void changedPolicyRejectsCanonicalAndOrderingCollisions() {
		final var owner = new MappedSuperclassColumnContainer( "CaseSensitiveBase" );
		owner.addColumn( new Column( EXACT.create( "code", false ) ) );
		owner.addColumn( new Column( EXACT.create( "CODE", false ) ) );
		final var restored = (MappedSuperclassColumnContainer) SerializationHelper.clone( owner );
		final var lifecycle = new ColumnNameLifecycle();
		lifecycle.addContainer( restored );
		assertThrows( MappingException.class, () -> lifecycle.restore( FOLDED ) );

		final var table = MappingTableHelper.table( "orm", "records", EXACT );
		final var index = new Index();
		index.setName( "idx" );
		index.setTable( table );
		index.addColumn( new Column( EXACT.create( "code", false ) ), "asc" );
		index.addColumn( new Column( EXACT.create( "CODE", false ) ), "desc" );
		table.addIndex( index );
		final var graph = new ColumnNameLifecycle();
		graph.addContainer( (org.hibernate.mapping.PhysicalTable) SerializationHelper.clone( table ) );
		assertThrows( MappingException.class, () -> graph.restore( FOLDED ) );
	}
}
