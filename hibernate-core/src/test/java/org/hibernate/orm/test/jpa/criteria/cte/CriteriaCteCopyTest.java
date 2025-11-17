/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.cte;

import org.hibernate.Session;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = BasicEntity.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18178" )
public class CriteriaCteCopyTest {
	@Test
	public void testValidRootQuery(EntityManagerFactoryScope scope) {
		executeTest( scope, (cb, cq) -> {
			final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			final Root<BasicEntity> root = query.from( BasicEntity.class );
			final Path<Integer> id = root.get( "id" );
			query.select( id.alias( "sub_id" ) ).where( cb.equal( id, 1 ) );
			return cq.with( query );
		} );
	}

	@Test
	public void testInvalidRootQuery(EntityManagerFactoryScope scope) {
		try {
			executeTest( scope, (cb, cq) -> {
				final JpaSubQuery<Integer> subquery = cq.subquery( Integer.class );
				final Root<BasicEntity> subRoot = subquery.from( BasicEntity.class );
				final Path<Integer> id = subRoot.get( "id" );
				subquery.select( id ).where( cb.equal( id, 1 ) ).alias( "sub_id" );
				return cq.with( subquery );
			} );
			fail( "Calling 'with' on a root query with a subquery instance should not be allowed" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( IllegalArgumentException.class ).hasMessageContaining(
					"Invalid query type provided to root query 'with' method"
			);
		}
	}

	@Test
	public void testValidSubQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = entityManager.unwrap( Session.class ).getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> cq = cb.createQuery( Integer.class );
			final JpaSubQuery<Integer> subquery = cq.subquery( Integer.class );
			final JpaSubQuery<Integer> cteSubquery = cq.subquery( Integer.class );
			final Root<BasicEntity> subRoot = cteSubquery.from( BasicEntity.class );
			final Path<Integer> id = subRoot.get( "id" );
			id.alias( "cte_id" );
			cteSubquery.select( id ).where( cb.equal( id, 1 ) );
			final JpaCteCriteria<Integer> cte = subquery.with( cteSubquery );
			final JpaRoot<Integer> cteFrom = subquery.from( cte );
			final JpaPath<Integer> cteId = cteFrom.get( "cte_id" );
			cteId.alias( "sub_id" );
			subquery.select( cteId );
			final JpaRoot<Integer> root = cq.from( subquery );
			cq.select( root.get( "sub_id" ) );
			assertThat( entityManager.createQuery( cq ).getSingleResult() ).isEqualTo( 1 );
		} );
	}

	private <T> void executeTest(EntityManagerFactoryScope scope, CteConsumer<T> cteConsumer) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder cb = entityManager.unwrap( Session.class ).getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> cq = cb.createQuery( Integer.class );
			final JpaCteCriteria<T> cte = cteConsumer.accept( cb, cq );
			final JpaRoot<T> root = cq.from( cte );
			cq.select( root.get( "sub_id" ) );
			assertThat( entityManager.createQuery( cq ).getSingleResult() ).isEqualTo( 1 );
		} );
	}

	interface CteConsumer<T> {
		JpaCteCriteria<T> accept(HibernateCriteriaBuilder cb, JpaCriteriaQuery<Integer> cq);
	}

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new BasicEntity( 1, "data_1" ) );
			entityManager.persist( new BasicEntity( 2, "data_2" ) );
			entityManager.persist( new BasicEntity( 3, "data_3" ) );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.createQuery( "delete from BasicEntity" ).executeUpdate() );
	}
}
