/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = EntityValuedPathInSubqueryTest.EntityA.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16397" )
public class EntityValuedPathInSubqueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA1 = new EntityA( "entitya_1", 1, null );
			session.persist( entityA1 );
			session.persist( new EntityA( "entitya_2", 2, entityA1 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "update EntityA set reference = null" ).executeUpdate();
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
		} );
	}

	@Test
	public void testReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String result = session.createQuery(
					"select a.name from EntityA a where a.amount = " +
					"(select max(b.amount) from EntityA b where a.reference is null and b.reference is null)",
					String.class
			).getSingleResult();
			assertThat( result ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testReferenceSelectEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA result = session.createQuery(
					"select a from EntityA a where a.amount = " +
					"(select max(b.amount) from EntityA b where a.reference is null and b.reference is null)",
					EntityA.class
			).getSingleResult();
			assertThat( result.getName() ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testExplicitFkReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String result = session.createQuery(
					"select a.name from EntityA a where a.amount = " +
					"(select max(b.amount) from EntityA b where a.reference.id is null and b.reference.id is null)",
					String.class
			).getSingleResult();
			assertThat( result ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String result = session.createQuery(
					"select a.name from EntityA a where a.amount = " +
					"(select max(b.amount) from EntityA b where a.reference.name = 'entitya_1' and b.reference is not null)",
					String.class
			).getSingleResult();
			assertThat( result ).isEqualTo( "entitya_2" );
		} );
	}

	@Test
	public void testImplicitJoinSelectReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA result = session.createQuery(
					"select a.reference from EntityA a where a.amount = " +
					"(select max(b.amount) from EntityA b where a.reference.name = 'entitya_1' and b.reference is not null)",
					EntityA.class
			).getSingleResult();
			assertThat( result.getName() ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testReferenceCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<String> query = cb.createQuery( String.class );
			final Root<EntityA> root = query.from( EntityA.class );
			// subquery with correlated root
			final Subquery<Integer> subquery = query.subquery( Integer.class );
			final Root<EntityA> correlatedRoot = subquery.correlate( root );
			final Root<EntityA> subRoot = subquery.from( EntityA.class );
			subquery.select( cb.max( subRoot.get( "amount" ) ) ).where( cb.and(
					cb.isNull( correlatedRoot.get( "reference" ) ),
					cb.isNull( subRoot.get( "reference" ) )
			) );
			// query
			query.select( root.get( "name" ) ).where( cb.equal( root.get( "amount" ), subquery ) );
			final String result = session.createQuery( query ).getSingleResult();
			assertThat( result ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testReferenceSelectEntityCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<EntityA> query = cb.createQuery( EntityA.class );
			final Root<EntityA> root = query.from( EntityA.class );
			// subquery with correlated root
			final Subquery<Integer> subquery = query.subquery( Integer.class );
			final Root<EntityA> correlatedRoot = subquery.correlate( root );
			final Root<EntityA> subRoot = subquery.from( EntityA.class );
			subquery.select( cb.max( subRoot.get( "amount" ) ) ).where( cb.and(
					cb.isNull( correlatedRoot.get( "reference" ) ),
					cb.isNull( subRoot.get( "reference" ) )
			) );
			// query
			query.select( root ).where( cb.equal( root.get( "amount" ), subquery ) );
			final EntityA result = session.createQuery( query ).getSingleResult();
			assertThat( result.getName() ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testExplicitFkReferenceCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<String> query = cb.createQuery( String.class );
			final Root<EntityA> root = query.from( EntityA.class );
			// subquery with correlated root
			final Subquery<Integer> subquery = query.subquery( Integer.class );
			final Root<EntityA> correlatedRoot = subquery.correlate( root );
			final Root<EntityA> subRoot = subquery.from( EntityA.class );
			subquery.select( cb.max( subRoot.get( "amount" ) ) ).where( cb.and(
					cb.isNull( correlatedRoot.get( "reference" ).get( "id" ) ),
					cb.isNull( subRoot.get( "reference" ).get( "id" ) )
			) );
			// query
			query.select( root.get( "name" ) ).where( cb.equal( root.get( "amount" ), subquery ) );
			final String result = session.createQuery( query ).getSingleResult();
			assertThat( result ).isEqualTo( "entitya_1" );
		} );
	}

	@Test
	public void testImplicitJoinCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<String> query = cb.createQuery( String.class );
			final Root<EntityA> root = query.from( EntityA.class );

			final Subquery<Integer> subquery = query.subquery( Integer.class );
			final Root<EntityA> subRoot = subquery.from( EntityA.class );
			subquery.select( cb.max( subRoot.get( "amount" ) ) ).where( cb.and(
					cb.equal( root.get( "reference" ).get( "name" ), "entitya_1" ),
					cb.isNotNull( subRoot.get( "reference" ) )
			) );

			query.select( root.get( "name" ) ).where( cb.equal( root.get( "amount" ), subquery ) );
			final String result = session.createQuery( query ).getSingleResult();
			assertThat( result ).isEqualTo( "entitya_2" );
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private Integer amount;

		@ManyToOne
		@JoinColumn( name = "reference" )
		private EntityA reference;

		public EntityA() {
		}

		public EntityA(String name, Integer amount, EntityA reference) {
			this.name = name;
			this.amount = amount;
			this.reference = reference;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Integer getAmount() {
			return amount;
		}

		public EntityA getReference() {
			return reference;
		}
	}
}
