/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.function;

import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for {@link SqmFunctionRegistry#retainOnly(Set)}.
 * Verifies that pruning works with a real SessionFactory and that
 * retained functions can be used in HQL queries.
 */
@DomainModel(annotatedClasses = SqmFunctionRegistryRetainOnlyTest.TestEntity.class)
@SessionFactory
@JiraKey("HHH-20839")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SqmFunctionRegistryRetainOnlyTest {

	@Test
	@Order(1)
	void retainOnly_reducesFunctionCount(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SessionFactoryImplementor sfi = session.getSessionFactory();
			SqmFunctionRegistry registry = sfi.getQueryEngine().getSqmFunctionRegistry();

			int initialSize = registry.getValidFunctionKeys().size();
			assertTrue( initialSize > 10, "Dialect should register many functions, got: " + initialSize );

			// Retain only core aggregate functions
			registry.retainOnly( Set.of( "count", "sum", "avg", "min", "max",
					"coalesce", "nullif", "cast", "lower", "upper", "trim",
					"substring", "concat", "length", "character_length",
					"abs", "mod", "sqrt", "floor", "ceiling",
					"current_date", "current_time", "current_timestamp",
					"extract", "position" ) );

			int prunedSize = registry.getValidFunctionKeys().size();
			assertTrue( prunedSize < initialSize,
					"Registry should be smaller after pruning: " + prunedSize + " >= " + initialSize );
		} );
	}

	@Test
	@Order(2)
	void retainOnly_retainedFunctionWorksInHql(SessionFactoryScope scope) {
		// Insert test data
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, "alpha" ) );
			session.persist( new TestEntity( 2L, "beta" ) );
			session.persist( new TestEntity( 3L, "gamma" ) );
		} );

		scope.inTransaction( session -> {
			SessionFactoryImplementor sfi = session.getSessionFactory();
			SqmFunctionRegistry registry = sfi.getQueryEngine().getSqmFunctionRegistry();

			// Retain count (and other core functions needed by Hibernate internally)
			registry.retainOnly( Set.of( "count", "sum", "avg", "min", "max",
					"coalesce", "nullif", "cast", "lower", "upper", "trim",
					"substring", "concat", "length", "character_length",
					"abs", "mod", "sqrt", "floor", "ceiling",
					"current_date", "current_time", "current_timestamp",
					"extract", "position" ) );

			// count() should still work
			Long result = session.createQuery(
					"select count(e) from TestEntity e", Long.class ).getSingleResult();
			assertEquals( 3L, result );
		} );

		// Cleanup
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from TestEntity" ).executeUpdate();
		} );
	}

	@Test
	@Order(3)
	void retainOnly_prunedFunctionNotAvailable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SessionFactoryImplementor sfi = session.getSessionFactory();
			SqmFunctionRegistry registry = sfi.getQueryEngine().getSqmFunctionRegistry();

			// Retain only core aggregate functions
			registry.retainOnly( Set.of( "count", "sum", "avg", "min", "max",
					"coalesce", "nullif", "cast" ) );

			// Functions not in the retained set should be gone
			assertNull( registry.findFunctionDescriptor( "json_object" ),
					"json_object should be pruned" );
			assertNull( registry.findFunctionDescriptor( "array_agg" ),
					"array_agg should be pruned" );

			// Retained functions should still be available
			assertNotNull( registry.findFunctionDescriptor( "count" ),
					"count should be retained" );
		} );
	}

	@Entity(name = "TestEntity")
	@Table(name = "sqm_function_test_entity")
	public static class TestEntity {
		@Id
		private Long id;
		private String name;

		public TestEntity() {
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
