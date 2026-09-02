/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

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
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedArrayType;
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
	@ParameterizedTest
	@MethodSource("tableDialects")
	void rendersStandardAndAggregateAwareTableVariants(Dialect dialect) {
		final String[] commands = dialect.getTableExporter().getSqlCreateStrings(
				new Table( "test", "orders" ),
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
				new Table( "test", "TYPE" ),
				null,
				new TestContext( dialect )
		)[0] ).contains( "\"TYPE\"" );
	}

	@Test
	void preservesSpannerIndexDropOrdering() {
		final Dialect dialect = new SpannerDialect();
		final Table table = new Table( "test", "orders" );
		final var index = table.getOrCreateIndex( "ix_orders_name" );
		index.addColumn( new Column( "name" ) );

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
	void preservesOracleArrayTypeRenderingBehindFacade() {
		final Dialect dialect = new OracleDialect();
		final Namespace namespace = mock( Namespace.class );
		when( namespace.getPhysicalName() ).thenReturn( new Namespace.Name( null, null ) );
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
		public boolean isMigration() {
			return false;
		}
	}
}
