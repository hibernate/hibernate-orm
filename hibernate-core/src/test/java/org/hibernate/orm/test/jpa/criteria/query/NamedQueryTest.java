/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.query;

import org.hibernate.query.sqm.internal.SqmQueryImpl;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = BasicEntity.class)
@JiraKey("HHH-16109")
public class NamedQueryTest {
	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new BasicEntity( 1, "test_1" ) );
			session.persist( new BasicEntity( 2, "test_2" ) );
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<BasicEntity> criteria = cb.createQuery( BasicEntity.class );
			criteria.select( criteria.from( BasicEntity.class ) );
			// Criteria
			final TypedQuery<BasicEntity> criteriaQuery = session.createQuery( criteria );
			scope.getSessionFactory().addNamedQuery( "criteria_query", criteriaQuery );
			// Criteria + limit / offset
			final TypedQuery<BasicEntity> criteriaQueryLimit = session.createQuery( criteria );
			criteriaQueryLimit.setFirstResult( 1 ).setMaxResults( 1 );
			scope.getSessionFactory().addNamedQuery( "criteria_query_limit", criteriaQueryLimit );
			// HQL
			final TypedQuery<BasicEntity> hqlQuery = session.createQuery( "from BasicEntity", BasicEntity.class );
			scope.getSessionFactory().addNamedQuery( "hql_query", hqlQuery );
			// HQL + limit / offset
			final TypedQuery<BasicEntity> hqlQueryLimit = session.createQuery( "from BasicEntity", BasicEntity.class );
			hqlQueryLimit.setFirstResult( 1 ).setMaxResults( 1 );
			scope.getSessionFactory().addNamedQuery( "hql_query_limit", hqlQueryLimit );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BasicEntity" ).executeUpdate() );
	}

	@Test
	public void testCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TypedQuery<BasicEntity> query = session.createNamedQuery( "criteria_query", BasicEntity.class );
			final SqmQueryImpl<BasicEntity> querySqm = (SqmQueryImpl<BasicEntity>) query;
			assertNull( querySqm.getQueryOptions().getLimit().getFirstRow() );
			assertNull( querySqm.getQueryOptions().getLimit().getMaxRows() );
			assertEquals( 2, query.getResultList().size() );
		} );
	}

	@Test
	public void testCriteriaLimit(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TypedQuery<BasicEntity> query = session.createNamedQuery( "criteria_query_limit", BasicEntity.class );
			final SqmQueryImpl<BasicEntity> querySqm = (SqmQueryImpl<BasicEntity>) query;
			assertEquals( 1, querySqm.getQueryOptions().getLimit().getFirstRow() );
			assertEquals( 1, querySqm.getQueryOptions().getLimit().getMaxRows() );
			assertEquals( 1, query.getResultList().size() );
		} );
	}

	@Test
	public void testHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TypedQuery<BasicEntity> query = session.createNamedQuery( "hql_query", BasicEntity.class );
			final SqmQueryImpl<BasicEntity> querySqm = (SqmQueryImpl<BasicEntity>) query;
			assertNull( querySqm.getQueryOptions().getLimit().getFirstRow() );
			assertNull( querySqm.getQueryOptions().getLimit().getMaxRows() );
			assertEquals( 2, query.getResultList().size() );
		} );
	}

	@Test
	public void testHqlLimit(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TypedQuery<BasicEntity> query = session.createNamedQuery( "hql_query_limit", BasicEntity.class );
			final SqmQueryImpl<BasicEntity> querySqm = (SqmQueryImpl<BasicEntity>) query;
			assertEquals( 1, querySqm.getQueryOptions().getLimit().getFirstRow() );
			assertEquals( 1, querySqm.getQueryOptions().getLimit().getMaxRows() );
			assertEquals( 1, query.getResultList().size() );
		} );
	}
}
