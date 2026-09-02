/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.util.List;

import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.IndexColumn;
import org.hibernate.dialect.schema.spi.IndexDdlRequest;
import org.hibernate.dialect.schema.spi.TableCreationKind;
import org.hibernate.dialect.schema.spi.TruncateMode;
import org.hibernate.dialect.schema.spi.TruncateRequest;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the standalone provider's schema strategy surface.
///
/// @author Steve Ebersole
class ExampleSchemaSupportTest {
	private final ExampleDialect dialect = new ExampleDialect();

	@Test
	void suppliesStableNonstandardStrategies() {
		assertSame( dialect.getIfExistsSupport(), dialect.getIfExistsSupport() );
		assertSame( dialect.getAlterTableSupport(), dialect.getAlterTableSupport() );
		assertSame( dialect.getTableCreationSupport(), dialect.getTableCreationSupport() );
		assertSame( dialect.getColumnDefinitionSupport(), dialect.getColumnDefinitionSupport() );
		assertSame( dialect.getIndexDdlSupport(), dialect.getIndexDdlSupport() );
		assertSame( dialect.getConstraintControlSupport(), dialect.getConstraintControlSupport() );
		assertSame( dialect.getTruncateSupport(), dialect.getTruncateSupport() );
		assertSame( dialect.getSchemaDropSupport(), dialect.getSchemaDropSupport() );
		assertSame( dialect.getTableMigrator(), dialect.getTableMigrator() );
		assertSame( dialect.getTableCleaner(), dialect.getTableCleaner() );
		assertSame( dialect.getTemporaryTableExporter(), dialect.getTemporaryTableExporter() );
	}

	@Test
	void rendersEveryFocusedStrategyAxis() {
		assertEquals(
				"fixture alter table if exists orders",
				dialect.getAlterTableSupport().alterTableCommand(
						"orders",
						dialect.getIfExistsSupport().alterTablePlacement()
				)
		);
		assertEquals( "fixture add column", dialect.getAlterTableSupport().addColumnPrefix() );
		assertEquals( "fixture column suffix", dialect.getAlterTableSupport().addColumnSuffix().trim() );
		assertEquals(
				"fixture alter column quantity as numeric(10,2) not null",
				dialect.getAlterTableSupport().alterColumnType(
						new AlterColumnTypeRequest( "quantity", "numeric(10,2)", "numeric(10,2) not null" )
				)
		);

		assertEquals(
				"create fixture multiset table",
				dialect.getTableCreationSupport().createTableCommand( TableCreationKind.MULTISET )
		);
		assertTrue( dialect.getTableCreationSupport().requiresViewColumnList() );

		final var appender = new StringBuilderSqlAppender();
		dialect.getColumnDefinitionSupport().appendDefinition(
				appender,
				new ColumnDefinitionRequest( "integer", "fixture_ci", false, "7", "quantity + 1" )
		);
		assertEquals(
				" fixture_type(integer) fixture_collate fixture_ci fixture_generated(quantity + 1)"
						+ " fixture_default 7 fixture_not_null",
				appender.toString()
		);

		final var indexRequest = new IndexDdlRequest(
				true,
				List.of( new IndexColumn( "quantity", true ) )
		);
		assertEquals( "create fixture unique index", dialect.getIndexDdlSupport().createCommand( indexRequest ) );
		assertEquals( " fixture nullable index tail", dialect.getIndexDdlSupport().createTail( indexRequest ) );

		assertEquals( ConstraintControlMode.GLOBAL, dialect.getConstraintControlSupport().constraintControlMode() );
		assertEquals( List.of( "fixture constraints off" ), dialect.getConstraintControlSupport().disableCommands() );
		assertEquals( List.of( "fixture constraints on" ), dialect.getConstraintControlSupport().enableCommands() );

		assertEquals( TruncateMode.MULTI_TABLE, dialect.getTruncateSupport().truncateMode() );
		assertEquals(
				List.of( "fixture empty orders then customers" ),
				dialect.getTruncateSupport().renderCommands( new TruncateRequest( List.of( "orders", "customers" ) ) )
		);
		assertEquals( List.of(), dialect.getTruncateSupport().renderCommands( new TruncateRequest( List.of() ) ) );
	}

	@Test
	void suppliesCompleteCustomMigratorAndCleaner() {
		final var table = new Table( "fixture", "orders" );
		assertArrayEquals(
				new String[] { "fixture migrate table orders" },
				dialect.getTableMigrator().getSqlAlterStrings( table, null, null, null )
		);

		final var foreignKey = new ForeignKey( table );
		foreignKey.setName( "fk_orders_customer" );
		assertEquals( ConstraintControlMode.PER_CONSTRAINT, dialect.getTableCleaner().constraintControlMode() );
		assertEquals( TruncateMode.MULTI_TABLE, dialect.getTableCleaner().truncateMode() );
		assertEquals( List.of( "fixture cleaner before" ), dialect.getTableCleaner().getSqlBeforeStrings() );
		assertEquals(
				List.of( "fixture disable fk_orders_customer" ),
				dialect.getTableCleaner().getSqlDisableConstraintStrings( foreignKey, null, null )
		);
		assertEquals(
				List.of( "fixture cleaner empty orders then customers" ),
				dialect.getTableCleaner().getSqlTruncateStrings(
						List.of( table, new Table( "fixture", "customers" ) ),
						null,
						null
				)
		);
		assertEquals( List.of( "fixture cleaner after" ), dialect.getTableCleaner().getSqlAfterStrings() );
	}

	@Test
	void rejectsInvalidRequestValues() {
		assertThrows( NullPointerException.class, () -> new AlterColumnTypeRequest( null, "integer", "integer" ) );
		assertThrows( NullPointerException.class, () -> new ColumnDefinitionRequest( null, null, true, null, null ) );
		assertThrows( IllegalArgumentException.class, () -> new IndexDdlRequest( false, List.of() ) );
		assertThrows( NullPointerException.class, () -> new TruncateRequest( java.util.Arrays.asList( "orders", null ) ) );
	}
}
