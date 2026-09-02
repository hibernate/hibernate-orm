/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies the standalone provider's supported standard-exporter subclass.
///
/// @author Steve Ebersole
public class ExampleSchemaExporterTest {
	@Test
	void suppliesAndExercisesSequenceNameSpecialization() {
		final ExampleDialect dialect = new ExampleDialect();
		final Sequence sequence = new Sequence(
				"fixture",
				null,
				null,
				Identifier.toIdentifier( "orders" ),
				5,
				10
		);

		assertSame( dialect.getSequenceExporter(), dialect.getSequenceExporter() );
		assertArrayEquals(
				new String[] { "create fixture sequence fixture_orders start 5 step 10" },
				dialect.getSequenceExporter().getSqlCreateStrings( sequence, null, new FixtureContext( dialect ) )
		);
	}

	private record FixtureContext(Dialect dialect) implements SqlStringGenerationContext {
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
