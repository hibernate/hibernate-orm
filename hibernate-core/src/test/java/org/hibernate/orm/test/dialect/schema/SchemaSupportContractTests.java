/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexColumn;
import org.hibernate.dialect.schema.spi.IndexDdlRequest;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.TableCreationKind;
import org.hibernate.dialect.schema.spi.TruncateRequest;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the focused schema contracts and stock Dialect behavior.
///
/// @author Steve Ebersole
@BaseUnitTest
class SchemaSupportContractTests {
	private final Dialect dialect = new TestDialect();

	@Test
	void suppliesStableStockStrategies() {
		assertSame( dialect, dialect.getAlterTableSupport() );
		assertSame( dialect, dialect.getTableCreationSupport() );
		assertSame( dialect, dialect.getColumnDefinitionSupport() );
		assertSame( dialect, dialect.getIndexDdlSupport() );
		assertSame( dialect, dialect.getConstraintControlSupport() );
		assertSame( dialect, dialect.getTruncateSupport() );
		assertSame( dialect.getIfExistsSupport(), dialect.getIfExistsSupport() );
		assertSame( dialect.getSchemaDropSupport(), dialect.getSchemaDropSupport() );
		assertSame( dialect.getTableMigrator(), dialect.getTableMigrator() );
		assertSame( dialect.getTableCleaner(), dialect.getTableCleaner() );
	}

	@Test
	void maintainedDialectProfilesAreStable() {
		for ( Dialect maintainedDialect : List.of(
				new CockroachDialect(),
				new DB2Dialect(),
				new H2Dialect(),
				new OracleDialect(),
				new PostgreSQLDialect(),
				new SQLServerDialect() ) ) {
			assertSame( maintainedDialect.getIfExistsSupport(), maintainedDialect.getIfExistsSupport() );
			assertSame( maintainedDialect.getSchemaDropSupport(), maintainedDialect.getSchemaDropSupport() );
		}
	}

	@Test
	@SuppressWarnings("removal")
	void oracleRetainsDdlThrottlingWorkaround() {
		assertFalse( dialect.throttleDdl() );
		assertTrue( new OracleDialect().throttleDdl() );
	}

	@Test
	void rendersStockSchemaGrammar() {
		assertEquals( "alter table orders", dialect.alterTableCommand( "orders", ExistenceCheckPlacement.NONE ) );
		assertEquals(
				"alter table if exists orders",
				dialect.alterTableCommand( "orders", ExistenceCheckPlacement.BEFORE_NAME )
		);
		assertEquals(
				"alter table orders if exists",
				dialect.alterTableCommand( "orders", ExistenceCheckPlacement.AFTER_NAME )
		);
		assertNull( dialect.alterColumnType( new AlterColumnTypeRequest( "quantity", "integer", "integer" ) ) );
		assertEquals( "create table", dialect.createTableCommand( TableCreationKind.STANDARD ) );
		assertEquals( "create table", dialect.createTableCommand( TableCreationKind.MULTISET ) );
		assertFalse( dialect.requiresViewColumnList() );

		final var appender = new StringBuilderSqlAppender();
		dialect.appendDefinition(
				appender,
				new ColumnDefinitionRequest( "integer", "en_US", false, "7", "quantity + 1" )
		);
		assertEquals(
				" integer collate en_US default 7 generated always as (quantity + 1) stored not null",
				appender.toString()
		);

		final var indexRequest = new IndexDdlRequest( true, List.of( new IndexColumn( "quantity", false ) ) );
		assertEquals( "create unique index", dialect.createCommand( indexRequest ) );
		assertEquals( "", dialect.createTail( indexRequest ) );
		assertEquals(
				List.of( "truncate table orders", "truncate table customers" ),
				dialect.renderCommands( new TruncateRequest( List.of( "orders", "customers" ) ) )
		);
		assertEquals( List.of(), dialect.renderCommands( new TruncateRequest( List.of() ) ) );
	}

	@Test
	void validatesAndDefensivelyCopiesRequestsAndProfiles() {
		assertThrows(
				NullPointerException.class,
				() -> new IfExistsSupport( null, ExistenceCheckPlacement.NONE,
						ExistenceCheckPlacement.NONE, ExistenceCheckPlacement.NONE )
		);
		assertThrows( NullPointerException.class, () -> new IndexColumn( null, false ) );
		assertThrows( IllegalArgumentException.class, () -> new IndexDdlRequest( false, List.of() ) );
		assertThrows(
				NullPointerException.class,
				() -> new TruncateRequest( Arrays.asList( "orders", null ) )
		);

		final var commands = new ArrayList<>( List.of( "prepare drop" ) );
		final var support = new SchemaDropSupport( commands, ConstraintDropMode.IMPLICIT, " cascade" );
		commands.add( "mutated" );
		assertEquals( List.of( "prepare drop" ), support.beforeDropCommands() );
		assertThrows( UnsupportedOperationException.class, () -> support.beforeDropCommands().add( "mutated" ) );
	}

	private static final class TestDialect extends Dialect {
		private TestDialect() {
			super( DatabaseVersion.make( 1 ) );
		}
	}
}
