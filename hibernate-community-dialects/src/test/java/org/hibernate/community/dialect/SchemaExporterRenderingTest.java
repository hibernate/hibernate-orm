/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.DialectTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies rendering by community schema-exporter implementations using the
/// supported strategy and composition surface.
///
/// @author Steve Ebersole
@BaseUnitTest
class SchemaExporterRenderingTest {
	@Test
	void communityDialectProfilesAreStable() {
		for ( Dialect dialect : java.util.List.of(
				new GaussDBDialect(),
				new H2LegacyDialect(),
				new InformixDialect(),
				new MySQLLegacyDialect(),
				new SQLServerLegacyDialect() ) ) {
			assertThat( dialect.getIfExistsSupport() ).isSameAs( dialect.getIfExistsSupport() );
			assertThat( dialect.getSchemaDropSupport() ).isSameAs( dialect.getSchemaDropSupport() );
		}
	}

	@Test
	void rendersGaussDbTableThroughSupportedExporter() {
		final Dialect dialect = new GaussDBDialect();
		assertThat( dialect.getTableExporter().getSqlCreateStrings(
				new Table( "test", "orders" ),
				null,
				new TestContext( dialect )
		)[0] ).startsWith( DialectTestSupport.createTableCommand( dialect ) ).contains( "orders" );
	}

	@Test
	void rendersInformixTableThroughSupportedExporter() {
		final Dialect dialect = new InformixDialect();
		assertThat( dialect.getTableExporter().getSqlCreateStrings(
				new Table( "test", "orders" ),
				null,
				new TestContext( dialect )
		)[0] ).startsWith( DialectTestSupport.createTableCommand( dialect ) ).contains( "orders" );
	}

	@Test
	void preservesFirebirdDescendingIndexRenderingAndStandardDrop() {
		final Dialect dialect = new FirebirdDialect();
		final var index = index( "ix_orders_name", "desc" );

		assertThat( dialect.getIndexExporter().getSqlCreateStrings(
				index,
				null,
				new TestContext( dialect )
		) ).containsExactly( "create desc index ix_orders_name on orders (name)" );
		assertThat( dialect.getIndexExporter().getSqlDropStrings(
				index,
				null,
				new TestContext( dialect )
		) ).containsExactly( "drop index ix_orders_name" );
	}

	@Test
	void preservesTeradataIndexRenderingAndStandardDrop() {
		final Dialect dialect = new TeradataDialect();
		final var index = index( "ix_orders_name", null );

		assertThat( dialect.getIndexExporter().getSqlCreateStrings(
				index,
				null,
				new TestContext( dialect )
		) ).containsExactly( "create index ix_orders_name(name) on orders" );
		assertThat( dialect.getIndexExporter().getSqlDropStrings(
				index,
				null,
				new TestContext( dialect )
		) ).containsExactly( "drop index orders.ix_orders_name" );
	}

	private static org.hibernate.mapping.Index index(String name, String order) {
		final Table table = new Table( "test", "orders" );
		final var index = table.getOrCreateIndex( name );
		index.addColumn( new Column( "name" ), order );
		return index;
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
