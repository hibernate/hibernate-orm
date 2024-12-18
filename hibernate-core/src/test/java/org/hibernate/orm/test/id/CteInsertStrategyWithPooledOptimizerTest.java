/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests sequence generation with pooled optimizer when using CTE-based batch inserts.
 * Verifies that ID allocation works correctly across regular persists and batch operations.
 *
 * @author Kowsar Atazadeh
 */
@JiraKey("HHH-18818")
@SessionFactory
@RequiresDialects(
		{
				@RequiresDialect(PostgreSQLDialect.class),
				@RequiresDialect(DB2Dialect.class),
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.PREFERRED_POOLED_OPTIMIZER, value = "pooled"),
		}
)
@DomainModel(annotatedClasses = { CteInsertStrategyWithPooledOptimizerTest.Dummy.class })
public class CteInsertStrategyWithPooledOptimizerTest {
	@Test
	void test(SessionFactoryScope scope) {
		// 9 rows inserted with IDs 1 to 9
		// Two calls to the DB for next sequence value generation: first returns 6, second returns 11
		// IDs 10 and 11 are still reserved for the PooledOptimizer
		scope.inTransaction( session -> {
			for ( var i = 1; i <= 9; i++ ) {
				Dummy d = new Dummy( "d" + i );
				session.persist( d );
				assertEquals( i, d.getId() );
			}
		} );

		// 9 rows inserted (using CteInsertStrategy) with IDs 12 to 20 (before the fix, IDs would be 16 to 24)
		// Two calls to the DB for next sequence value generation: first returns 16, second returns 21
		scope.inTransaction( session -> {
			session.createMutationQuery( "INSERT INTO Dummy (name) SELECT d.name FROM Dummy d" ).
					executeUpdate();
		} );

		// Two rows inserted with the reserved IDs 10 and 11
		scope.inTransaction( session -> {
			for ( var i = 10; i <= 11; i++ ) {
				Dummy d = new Dummy( "d" + i );
				session.persist( d );
				assertEquals( i, d.getId() );
			}
		} );

		// One more row inserted with ID 22
		// One call to the DB for next sequence value generation which returns 26 (IDs 22-26 allocated)
		// Before the fix, this would result in a duplicate ID error (since batch insert used IDs 16 to 24)
		scope.inTransaction( session -> {
			Dummy d22 = new Dummy( "d22" );
			session.persist( d22 );
			assertEquals( 22, d22.getId() );
		} );
	}

	@Entity(name = "Dummy")
	static class Dummy {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dummy_seq")
		@SequenceGenerator(name = "dummy_seq", sequenceName = "dummy_seq", allocationSize = 5)
		private Long id;

		private String name;

		public Dummy() {
		}

		public Dummy(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
