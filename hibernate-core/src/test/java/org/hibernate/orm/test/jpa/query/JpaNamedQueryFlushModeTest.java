/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.QueryFlushMode;

import org.hibernate.FlushMode;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = JpaNamedQueryFlushModeTest.TestEntity.class)
@JiraKey("HHH-20366")
class JpaNamedQueryFlushModeTest {
	@Test
	void namedQueryFlushMode(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final Query<?> query = entityManager.createNamedQuery( "JpaNamedQueryFlush", TestEntity.class )
					.unwrap( Query.class );

			assertEquals( QueryFlushMode.FLUSH, query.getQueryFlushMode() );
			assertEquals( FlushMode.ALWAYS, query.getEffectiveFlushMode() );
		} );
	}

	@Test
	void namedQueryNoFlushMode(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final Query<?> query = entityManager.createNamedQuery( "JpaNamedQueryNoFlush", TestEntity.class )
					.unwrap( Query.class );

			assertEquals( QueryFlushMode.NO_FLUSH, query.getQueryFlushMode() );
			assertEquals( FlushMode.MANUAL, query.getEffectiveFlushMode() );
		} );
	}

	@Test
	void namedNativeQueryFlushMode(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final Query<?> query = entityManager.createNamedQuery( "JpaNamedNativeQueryFlush", TestEntity.class )
					.unwrap( Query.class );

			assertEquals( QueryFlushMode.FLUSH, query.getQueryFlushMode() );
			assertEquals( FlushMode.ALWAYS, query.getEffectiveFlushMode() );
		} );
	}

	@Test
	void namedNativeQueryNoFlushMode(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final Query<?> query = entityManager.createNamedQuery( "JpaNamedNativeQueryNoFlush", TestEntity.class )
					.unwrap( Query.class );

			assertEquals( QueryFlushMode.NO_FLUSH, query.getQueryFlushMode() );
			assertEquals( FlushMode.MANUAL, query.getEffectiveFlushMode() );
		} );
	}

	@Entity(name = "JpaNamedFlushModeEntity")
	@jakarta.persistence.NamedQuery(
			name = "JpaNamedQueryFlush",
			query = "select e from JpaNamedFlushModeEntity e",
			flush = QueryFlushMode.FLUSH
	)
	@jakarta.persistence.NamedQuery(
			name = "JpaNamedQueryNoFlush",
			query = "select e from JpaNamedFlushModeEntity e",
			flush = QueryFlushMode.NO_FLUSH
	)
	@jakarta.persistence.NamedNativeQuery(
			name = "JpaNamedNativeQueryFlush",
			query = "select * from JpaNamedFlushModeEntity",
			resultClass = TestEntity.class,
			flush = QueryFlushMode.FLUSH
	)
	@jakarta.persistence.NamedNativeQuery(
			name = "JpaNamedNativeQueryNoFlush",
			query = "select * from JpaNamedFlushModeEntity",
			resultClass = TestEntity.class,
			flush = QueryFlushMode.NO_FLUSH
	)
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;
	}
}
