/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.testing.util.MappingTableHelper;


import java.util.stream.Stream;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UserDefinedArrayType;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.relational.naming.spi.QualifiedPhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.DialectTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies representative rendering through the supported schema-exporter
/// supply points retained by maintained Dialects.
///
/// @author Steve Ebersole
@BaseUnitTest
public class SchemaExporterRenderingTests {
	private static final org.hibernate.relational.naming.spi.PhysicalName.Factory COLUMN_NAMES =
			new org.hibernate.relational.naming.spi.PhysicalName.Factory(
					(text, quoted) -> quoted ? text : text.toUpperCase( java.util.Locale.ROOT ) );

	@ParameterizedTest
	@MethodSource("tableDialects")
	void rendersStandardAndAggregateAwareTableVariants(Dialect dialect) {
		final String[] commands = dialect.getTableExporter().getSqlCreateStrings(
				MappingTableHelper.table( "test", "orders", new PhysicalName.Factory( (text, quoted) -> text ) ),
				null,
				new TestContext( dialect )
		);

		assertThat( commands ).hasSize( 1 );
		assertThat( commands[0] )
				.startsWith( DialectTestSupport.createTableCommand( dialect ) )
				.contains( "orders" );
	}

	@Test
	void preservesHanaTypeTableQuoting() {
		final Dialect dialect = new HANADialect();
		assertThat( dialect.getTableExporter().getSqlCreateStrings(
				MappingTableHelper.table( "test", "TYPE", new PhysicalName.Factory( (text, quoted) -> text ) ),
				null,
				new TestContext( dialect )
		)[0] ).contains( "\"TYPE\"" );
	}

	@Test
	void preservesSpannerIndexDropOrdering() {
		final Dialect dialect = new SpannerDialect();
		final var table = MappingTableHelper.table( "test", "orders", new PhysicalName.Factory( (text, quoted) -> text ) );
		final var index = table.getOrCreateIndex( "ix_orders_name" );
		index.addColumn( new Column( MappingTableHelper.columnName( "name", COLUMN_NAMES ) ) );

		assertThat( dialect.getTableExporter().getSqlDropStrings(
				table,
				null,
				new TestContext( dialect )
		) ).containsExactly(
				"drop index if exists ix_orders_name",
				"drop table if exists orders"
		);
	}

	@Test
	void preservesNamedViewOptionsAndComments() {
		final var dialect = new PostgreSQLDialect();
		final var context = new TestContext( dialect );
		final var view = new org.hibernate.mapping.DatabaseView( "test",
				new QualifiedPhysicalName( null, null, context.getPhysicalNameFactory().create( "report", true ) ),
				"select id from orders" );
		view.setOptions( "with local check option" );
		view.setComment( "Order report" );
		assertThat( dialect.getTableExporter().getSqlCreateStrings( view, null, context ) )
				.containsExactly( "create view \"report\" as select id from orders with local check option",
						"comment on table \"report\" is 'Order report'" );
	}

	@Test
	void preservesOracleArrayTypeRenderingBehindFacade() {
		final Dialect dialect = new OracleDialect();
		final Namespace namespace = mock( Namespace.class );
		when( namespace.getPhysicalName() ).thenReturn( new org.hibernate.boot.model.relational.PhysicalNamespaceName( null, null ) );
		final UserDefinedArrayType arrayType = new UserDefinedArrayType(
				"test",
				namespace,
				Identifier.toIdentifier( "phone_numbers" )
		);
		arrayType.setElementTypeName( "varchar2(32)" );

		assertThat( dialect.getUserDefinedTypeExporter().getSqlCreateStrings(
				arrayType,
				null,
				new TestContext( dialect )
		) ).containsExactly( "create or replace type phone_numbers as table of varchar2(32)" );
	}

	private static Stream<Dialect> tableDialects() {
		return Stream.of(
				new H2Dialect(),
				new DB2Dialect(),
				new PostgreSQLDialect(),
				new OracleDialect(),
				new SQLServerDialect()
		);
	}

	private record TestContext(Dialect dialect) implements SqlStringGenerationContext {
		@Override
		public PhysicalName.Factory getPhysicalNameFactory() {
			return new PhysicalName.Factory( (text, quoted) -> text );
		}

		@Override
		public Dialect getDialect() {
			return dialect;
		}

		@Override
		public Identifier toIdentifier(String text) {
			return Identifier.toIdentifier( text );
		}

		@Override
		public Identifier getDefaultCatalog() {
			return null;
		}

		@Override
		public Identifier getDefaultSchema() {
			return null;
		}

		@Override
		public String format(QualifiedTableName qualifiedName) {
			return qualifiedName.render();
		}

		@Override
		public String format(QualifiedSequenceName qualifiedName) {
			return qualifiedName.render();
		}

		@Override
		public String format(QualifiedName qualifiedName) {
			return qualifiedName.render();
		}

		@Override
		public String formatWithoutCatalog(QualifiedSequenceName qualifiedName) {
			return qualifiedName.render();
		}

		@Override
		public String format(QualifiedPhysicalName name) {
			return new org.hibernate.engine.jdbc.env.internal.QualifiedObjectNameFormatterStandardImpl(
					org.hibernate.engine.jdbc.env.spi.NameQualifierSupport.BOTH, ".", false ).format( name, dialect );
		}

		@Override
		public String formatWithoutCatalog(QualifiedPhysicalName name) { return name.render(); }

		@Override
		public boolean isMigration() {
			return false;
		}
	}
}
