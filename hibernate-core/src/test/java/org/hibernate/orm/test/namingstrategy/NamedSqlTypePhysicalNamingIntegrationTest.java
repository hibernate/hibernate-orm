/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Exercises exported physical type names through real SQL and JDBC operations.
///
/// @author Steve Ebersole
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(value = OracleDialect.class, majorVersion = 23)
@ServiceRegistry(settings = @Setting(name = "hibernate.physical_naming_strategy",
		value = "org.hibernate.orm.test.namingstrategy.NamedSqlTypePhysicalNamingIntegrationTest$Naming"))
@DomainModel(annotatedClasses = NamedSqlTypePhysicalNamingIntegrationTest.Sample.class)
@SessionFactory
class NamedSqlTypePhysicalNamingIntegrationTest {
	@Test
	void namedTypesAndArrayHelpers(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var sample = new Sample();
			sample.id = 1;
			sample.state = State.ACTIVE;
			sample.numbers = new Integer[] { 1, 2 };
			session.persist( sample );
			final var nullArray = new Sample();
			nullArray.id = 2;
			nullArray.state = State.INACTIVE;
			session.persist( nullArray );
		} );
		try {
			scope.inTransaction( session -> {
				final var sample = session.createQuery(
						"from NamingSample where state = ACTIVE", Sample.class ).getSingleResult();
				assertThat( sample.numbers ).containsExactly( 1, 2 );
				assertThat( session.createQuery(
						"select array_position(numbers, 2) from NamingSample where id = 1", Integer.class )
						.getSingleResult() ).isEqualTo( 2 );
				assertThat( session.find( Sample.class, 2L ).numbers ).isNull();
			} );
		}
		finally {
			scope.dropData();
		}
	}

	public static class Naming extends PhysicalNamingStrategyStandardImpl {
		@Override @Nonnull
		public PhysicalName toPhysicalEnumName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), true );
		}

		@Override @Nonnull
		public PhysicalName toPhysicalArrayName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), true );
		}

		@Override @Nonnull
		public PhysicalName toPhysicalStructName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), true );
		}
	}

	enum State { ACTIVE, INACTIVE }

	@Entity(name = "NamingSample")
	static class Sample {
		@Id long id;
		@JdbcTypeCode(SqlTypes.NAMED_ENUM) State state;
		Integer[] numbers;
	}
}
