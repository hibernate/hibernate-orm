/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.reader;

import org.hibernate.tool.reveng.test.utils.PhysicalNameHelper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.mapping.Column;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.tool.reveng.api.core.ForeignKeyDefinition;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.core.RevengMetadataCollector;
import org.hibernate.tool.reveng.internal.core.dialect.JDBCMetaDataDialect;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.internal.core.strategy.DelegatingStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Characterizes the source-to-JDBC boundary, including existing lookup limitations.
///
/// @author Steve Ebersole
class ForeignKeyDefinitionResolutionTest {
	private static final org.hibernate.relational.naming.spi.PhysicalName.Factory COLUMN_NAMES =
			new org.hibernate.relational.naming.spi.PhysicalName.Factory(
					(text, quoted) -> text );


	@Test
	void resolvesCustomDefinitionsToDiscoveredObjectsThroughDelegation() {
		final var collector = collector();
		final var parent = collector.addTable( selector( "parent" ) );
		final var child = collector.addTable( selector( "child" ) );
		final var id = new Column( PhysicalNameHelper.columnName( "id", COLUMN_NAMES ) );
		final var code = new Column( PhysicalNameHelper.columnName( "code", COLUMN_NAMES ) );
		final var parentId = new Column( PhysicalNameHelper.columnName( "parent_id", COLUMN_NAMES ) );
		final var parentCode = new Column( PhysicalNameHelper.columnName( "parent_code", COLUMN_NAMES ) );
		parent.addColumn( id );
		parent.addColumn( code );
		child.addColumn( parentId );
		child.addColumn( parentCode );
		final var definition = new ForeignKeyDefinition( "fk", selector( "child" ), selector( "parent" ),
				List.of( pair( "parent_code", "code" ), pair( "parent_id", "id" ) ) );
		final var strategy = strategy( List.of( definition ) );
		final var info = ForeignKeyProcessor.create( dialect( List.of() ), strategy, null, null, collector )
				.processForeignKeys( parent );
		assertTrue( child.getForeignKeys().isEmpty() );
		info.process( strategy );
		final var key = child.getForeignKeys().iterator().next();
		assertSame( child, key.getTable() );
		assertSame( parent, key.getReferencedTable() );
		assertSame( parentCode, key.getColumns().get( 0 ) );
		assertSame( parentId, key.getColumns().get( 1 ) );
		assertSame( code, key.getReferencedColumns().get( 0 ) );
		assertSame( id, key.getReferencedColumns().get( 1 ) );
	}

	@Test
	void ignoresUnknownDependentAndUnrelatedTargetTables() {
		final var collector = collector();
		final var parent = collector.addTable( selector( "parent" ) );
		collector.addTable( selector( "child" ) );
		final var strategy = strategy( List.of(
				definition( "unknown", "missing", "parent", "id", "id" ),
				definition( "unrelated", "child", "other", "id", "id" ) ) );
		assertTrue( ForeignKeyProcessor.create( dialect( List.of() ), strategy, null, null, collector )
				.processForeignKeys( parent ).process( strategy ).isEmpty() );
	}

	@Test
	void retainsNamedJdbcConflictAndMultipleUnnamedKeyLimitation() {
		final var collector = collector();
		final var parent = collector.addTable( selector( "parent" ) );
		collector.addTable( selector( "child" ) );
		final var strategy = strategy( List.of( definition( "fk", "child", "parent", "a", "id" ) ) );
		final var dialect = dialect( List.of( Map.of(
				"FK_NAME", "fk", "FKTABLE_NAME", "child", "FKCOLUMN_NAME", "a", "PKCOLUMN_NAME", "id" ) ) );
		assertThrows( MappingException.class, () -> ForeignKeyProcessor.create( dialect, strategy, null, null, collector )
				.processForeignKeys( parent ) );
		final var unnamed = strategy( List.of(
				definition( null, "child", "parent", "a", "id" ),
				definition( null, "child", "parent", "b", "id" ) ) );
		assertThrows( MappingException.class, () -> ForeignKeyProcessor.create( dialect( List.of() ), unnamed, null, null, collector )
				.processForeignKeys( parent ) );
	}

	@Test
	void retainsMissingColumnFallbackAndExistingLossOfSourceQuotingAtLookup() {
		final var collector = collector();
		final var parent = collector.addTable( selector( "parent" ) );
		collector.addTable( selector( "child" ) );
		final var strategy = strategy( List.of( definition( "fk", "child", "parent", "`Missing`", "`Code`" ) ) );
		final var info = ForeignKeyProcessor.create( dialect( List.of() ), strategy, null, null, collector )
				.processForeignKeys( parent );
		assertEquals( "Missing", info.dependentColumns.get( "fk" ).get( 0 ).getName() );
		assertFalse( info.dependentColumns.get( "fk" ).get( 0 ).isQuoted() );
		assertEquals( "Code", info.referencedColumns.get( "fk" ).get( 0 ).getName() );
		assertFalse( info.referencedColumns.get( "fk" ).get( 0 ).isQuoted() );
	}

	private static RevengMetadataCollector collector() {
		return new RevengMetadataCollector( new PhysicalName.Factory( (text, quoted) -> text ) );
	}

	private static TableIdentifier selector(String name) {
		return TableIdentifier.create( null, null, name );
	}

	private static ForeignKeyDefinition.ColumnReference pair(String column, String referencedColumn) {
		return new ForeignKeyDefinition.ColumnReference( column, referencedColumn );
	}

	private static ForeignKeyDefinition definition(String name, String table, String target, String column, String referencedColumn) {
		return new ForeignKeyDefinition( name, selector( table ), selector( target ), List.of( pair( column, referencedColumn ) ) );
	}

	private static DelegatingStrategy strategy(List<ForeignKeyDefinition> definitions) {
		return new DelegatingStrategy( new DefaultStrategy() {
			@Override
			public List<ForeignKeyDefinition> getForeignKeys(TableIdentifier table) {
				return definitions;
			}
		} );
	}

	private static JDBCMetaDataDialect dialect(List<Map<String, Object>> exportedKeys) {
		return new JDBCMetaDataDialect() {
			@Override
			public Iterator<Map<String, Object>> getExportedKeys(String catalog, String schema, String table) {
				return exportedKeys.iterator();
			}

			@Override
			public boolean needQuote(String name) {
				return false;
			}

			@Override
			public void close(Iterator<?> iterator) {
			}
		};
	}
}
