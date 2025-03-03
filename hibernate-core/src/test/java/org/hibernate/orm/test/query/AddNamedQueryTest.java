/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.jpa.Distributor;
import org.hibernate.orm.test.jpa.Item;
import org.hibernate.orm.test.jpa.Wallet;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;

import static org.hibernate.jpa.HibernateHints.HINT_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@link jakarta.persistence.EntityManagerFactory#addNamedQuery} handling.
 *
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = {
				Item.class,
				Distributor.class,
				Wallet.class
		}
)
public class AddNamedQueryTest {

	@Test
	public void basicTest(EntityManagerFactoryScope scope) {
		// just making sure we can add one and that it is usable when we get it back
		scope.inEntityManager(
				entityManager -> {
					Query query = entityManager.createQuery( "from Item" );
					final String name = "myBasicItemQuery";
					entityManager.getEntityManagerFactory().addNamedQuery( name, query );
					Query query2 = entityManager.createNamedQuery( name );
					query2.getResultList();
				}
		);
	}

	@Test
	public void replaceTest(EntityManagerFactoryScope scope) {

		scope.inEntityManager(
				em -> {
					final String name = "myReplaceItemQuery";

					// create a jpql query
					String sql = "from Item";
					Query query = em.createQuery( sql );
					query.setHint( "org.hibernate.comment", sql );
					em.getEntityManagerFactory().addNamedQuery( name, query );
					query = em.createNamedQuery( name );
					assertEquals( sql, query.getHints().get( "org.hibernate.comment" ) );
					assertEquals( 0, query.getResultList().size() );

					// create a native query and replace the previous jpql
					sql = "select * from Item";
					query = em.createNativeQuery( sql, Item.class );
					query.setHint( "org.hibernate.comment", sql );
					em.getEntityManagerFactory().addNamedQuery( name, query );
					query = em.createNamedQuery( name );
					assertEquals( sql, query.getHints().get( "org.hibernate.comment" ) );
					assertEquals( 0, query.getResultList().size() );

					// define back a named query
					sql = "from Item";
					query = em.createQuery( sql );
					query.setHint( "org.hibernate.comment", sql );
					em.getEntityManagerFactory().addNamedQuery( name, query );
					query = em.createNamedQuery( name );
					assertEquals( sql, query.getHints().get( "org.hibernate.comment" ) );
					assertEquals( 0, query.getResultList().size() );

				}
		);
	}

	@Test
	public void testLockModeHandling(EntityManagerFactoryScope scope) {
		final String name = "lock-mode-handling";
		scope.inTransaction(
				em -> {
					Query q = em.createQuery( "from Item" );
					assertEquals( LockModeType.NONE, q.getLockMode() );
					q.setLockMode( LockModeType.OPTIMISTIC );
					assertEquals( LockModeType.OPTIMISTIC, q.getLockMode() );
					em.getEntityManagerFactory().addNamedQuery( name, q );

					// first, lets check the underlying stored query def
					SessionFactoryImplementor sfi = scope.getEntityManagerFactory()
							.unwrap( SessionFactoryImplementor.class );
					NamedSqmQueryMemento<?> def = sfi.getQueryEngine()
							.getNamedObjectRepository()
							.getSqmQueryMemento( name );
					assertEquals( LockMode.OPTIMISTIC, def.getLockOptions().getLockMode() );

					// then lets create a query by name and check its setting
					q = em.createNamedQuery( name );
					assertEquals(
							LockMode.OPTIMISTIC,
							q.unwrap( org.hibernate.query.Query.class ).getLockOptions().getLockMode()
					);
					assertEquals( LockModeType.OPTIMISTIC, q.getLockMode() );
				}
		);
	}

	@Test
	public void testFlushModeHandling(EntityManagerFactoryScope scope) {
		final String name = "flush-mode-handling";

		scope.inTransaction(
				em -> {
					Query q = em.createQuery( "from Item" );
					assertEquals( FlushModeType.AUTO, q.getFlushMode() );
					q.setFlushMode( FlushModeType.COMMIT );
					assertEquals( FlushModeType.COMMIT, q.getFlushMode() );
					em.getEntityManagerFactory().addNamedQuery( name, q );

					// first, lets check the underlying stored query def
					SessionFactoryImplementor sfi = scope.getEntityManagerFactory()
							.unwrap( SessionFactoryImplementor.class );
					NamedSqmQueryMemento<?> def = sfi.getQueryEngine()
							.getNamedObjectRepository()
							.getSqmQueryMemento( name );
					assertEquals( FlushMode.COMMIT, def.getFlushMode() );

					// then lets create a query by name and check its setting
					q = em.createNamedQuery( name );
					assertEquals(
							FlushMode.COMMIT,
							q.unwrap( org.hibernate.query.Query.class ).getHibernateFlushMode()
					);
					assertEquals( FlushModeType.COMMIT, q.getFlushMode() );
				}
		);
	}

	@Test
	public void testConfigValueHandling(EntityManagerFactoryScope scope) {
		final String name = "itemJpaQueryWithLockModeAndHints";
		scope.inTransaction(
				em -> {
					Query query = em.createNamedQuery( name );
					org.hibernate.query.Query hibernateQuery = (org.hibernate.query.Query) query;
					// assert the state of the query config settings based on the initial named query
					//
					//		NOTE: here we check "query options" via the Hibernate contract (allowing nullness checking); see below for access via the JPA contract
					assertNull( hibernateQuery.getQueryOptions().getFirstRow() );
					assertNull( hibernateQuery.getQueryOptions().getMaxRows() );
					assertEquals( FlushMode.MANUAL, hibernateQuery.getHibernateFlushMode() );
					assertEquals( FlushModeType.COMMIT, hibernateQuery.getFlushMode() );
					assertEquals( CacheMode.IGNORE, hibernateQuery.getCacheMode() );
					assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getLockOptions().getLockMode() );
					assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getHibernateLockMode() );
					// jpa timeout is in milliseconds, whereas Hibernate's is in seconds
					assertEquals( (Integer) 3, hibernateQuery.getTimeout() );

					query.setHint( HINT_TIMEOUT, 10 );
					em.getEntityManagerFactory().addNamedQuery( name, query );

					query = em.createNamedQuery( name );
					hibernateQuery = (org.hibernate.query.Query) query;
					// assert the state of the query config settings based on the initial named query
					//
					//		NOTE: here we check "query options" via the JPA contract
					assertEquals( 0, hibernateQuery.getFirstResult() );
					assertEquals( Integer.MAX_VALUE, hibernateQuery.getMaxResults() );
					assertEquals( FlushMode.MANUAL, hibernateQuery.getHibernateFlushMode() );
					assertEquals( FlushModeType.COMMIT, hibernateQuery.getFlushMode() );
					assertEquals( CacheMode.IGNORE, hibernateQuery.getCacheMode() );
					assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getLockOptions().getLockMode() );
					assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getHibernateLockMode() );
					assertEquals( (Integer) 10, hibernateQuery.getTimeout() );

					query.setHint( HINT_SPEC_QUERY_TIMEOUT, 10000 );
					em.getEntityManagerFactory().addNamedQuery( name, query );

					query = em.createNamedQuery( name );
					hibernateQuery = (org.hibernate.query.Query) query;
					// assert the state of the query config settings based on the initial named query
					assertEquals( 0, hibernateQuery.getFirstResult() );
					assertEquals( Integer.MAX_VALUE, hibernateQuery.getMaxResults() );
					assertEquals( FlushMode.MANUAL, hibernateQuery.getHibernateFlushMode() );
					assertEquals( FlushModeType.COMMIT, hibernateQuery.getFlushMode() );
					assertEquals( CacheMode.IGNORE, hibernateQuery.getCacheMode() );
					assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getLockOptions().getLockMode() );
					assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getHibernateLockMode() );
					assertEquals( (Integer) 10, hibernateQuery.getTimeout() );

					query.setFirstResult( 51 );
					em.getEntityManagerFactory().addNamedQuery( name, query );

					query = em.createNamedQuery( name );
					hibernateQuery = (org.hibernate.query.Query) query;
					// assert the state of the query config settings based on the initial named query
					assertEquals( 51, hibernateQuery.getFirstResult() );
					assertEquals( Integer.MAX_VALUE, hibernateQuery.getMaxResults() );
					assertEquals( FlushMode.MANUAL, hibernateQuery.getHibernateFlushMode() );
					assertEquals( FlushModeType.COMMIT, hibernateQuery.getFlushMode() );
					assertEquals( CacheMode.IGNORE, hibernateQuery.getCacheMode() );
					assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getLockOptions().getLockMode() );
					assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getHibernateLockMode() );
					assertEquals( (Integer) 10, hibernateQuery.getTimeout() );
				}
		);
	}
}
