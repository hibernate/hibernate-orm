/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.validation.constraints.NotNull;

@Jpa(
		annotatedClasses = {
				SubqueryJoinTest.TestContext.class,
				SubqueryJoinTest.TestUser.class,
		}
)
public class SubqueryJoinTest {

	@Test
	@JiraKey(value = "HHH-15260")
	public void subqueryJoinTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

					final CriteriaQuery<TestContext> c = cb.createQuery( TestContext.class );
					final Root<TestContext> from = c.from( TestContext.class );
					from.join( "user" );

					final Subquery<TestUser.TestMarker> subQuery = c.subquery( TestUser.TestMarker.class );
					final Root<TestUser> sRoot = subQuery.from( TestUser.class );
					final Join<TestUser, TestUser.TestMarker> join = sRoot.join( "markers" );
					subQuery.where( cb.equal( sRoot.get( "id" ), from.get( "user" ).get( "id" ) ) );
					subQuery.select( join );
					c.where( cb.exists( subQuery ).not() );

					entityManager.createQuery( c ).getResultList();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-15260")
	public void subqueryJoinTest2(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

					final CriteriaQuery<TestContext> c = cb.createQuery( TestContext.class );
					final Root<TestContext> from = c.from( TestContext.class );
					from.join( "user" ).join( "context" );

					final Subquery<TestUser.TestMarker> subQuery = c.subquery( TestUser.TestMarker.class );
					final Root<TestUser> sRoot = subQuery.from( TestUser.class );
					final Join<TestUser, TestUser.TestMarker> join = sRoot.join( "markers" );
					sRoot.join( "context" );
					subQuery.where( cb.equal( sRoot.get( "id" ), from.get( "user" ).get( "context" ).get( "user" ).get( "id" ) ) );
					subQuery.select( join );
					c.where( cb.exists( subQuery ).not() );

					entityManager.createQuery( c ).getResultList();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-15260")
	public void subqueryJoinTest3(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

					final CriteriaQuery<TestContext> c = cb.createQuery( TestContext.class );
					final Root<TestContext> from = c.from( TestContext.class );
					from.join( "user" ).join( "context" );

					final Subquery<TestUser.TestMarker> subQuery = c.subquery( TestUser.TestMarker.class );
					final Root<TestUser> sRoot = subQuery.from( TestUser.class );
					final Join<TestUser, TestUser.TestMarker> join = sRoot.join( "markers" );
					sRoot.join( "context" );
					subQuery.where( cb.equal( sRoot.get( "id" ), from.get( "user" ).get( "context" ).get( "id" ) ) );
					subQuery.select( join );
					c.where( cb.exists( subQuery ).not() );

					entityManager.createQuery( c ).getResultList();
				}
		);
	}

	@Entity(name = "TestContext")
	public static class TestContext {

		@Id
		private Integer id;

		@NotNull
		@OneToOne(optional = false)
		private TestUser user;

	}

	@Entity(name = "TestUser")
	public static class TestUser {

		@Id
		private Integer id;

		@ManyToOne
		private TestContext context;

		@ElementCollection
		@Enumerated(EnumType.STRING)
		private List<TestMarker> markers = new ArrayList<>();

		public enum TestMarker {
			TEST
		}
	}

}
