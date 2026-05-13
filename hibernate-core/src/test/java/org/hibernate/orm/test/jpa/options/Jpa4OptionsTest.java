/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.options;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = Jpa4OptionsTest.Book.class)
class Jpa4OptionsTest {
	@Test
	void entityManagerOptions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.addOption( CacheRetrieveMode.BYPASS );
			entityManager.addOption( CacheStoreMode.REFRESH );
			entityManager.addOption( FlushModeType.COMMIT );

			assertEquals( CacheRetrieveMode.BYPASS, entityManager.getCacheRetrieveMode() );
			assertEquals( CacheStoreMode.REFRESH, entityManager.getCacheStoreMode() );
			assertEquals( FlushModeType.COMMIT, entityManager.getFlushMode() );

			assertTrue( entityManager.getOptions().contains( CacheRetrieveMode.BYPASS ) );
			assertTrue( entityManager.getOptions().contains( CacheStoreMode.REFRESH ) );
			assertTrue( entityManager.getOptions().contains( FlushModeType.COMMIT ) );
		} );
	}

	@Test
	void entityManagerCreationOptions(EntityManagerFactoryScope scope) {
		try ( var entityManager = scope.getEntityManagerFactory()
				.createEntityManager( CacheRetrieveMode.BYPASS, CacheStoreMode.REFRESH, FlushModeType.COMMIT ) ) {

			assertEquals( CacheRetrieveMode.BYPASS, entityManager.getCacheRetrieveMode() );
			assertEquals( CacheStoreMode.REFRESH, entityManager.getCacheStoreMode() );
			assertEquals( FlushModeType.COMMIT, entityManager.getFlushMode() );

			assertTrue( entityManager.getOptions().contains( CacheRetrieveMode.BYPASS ) );
			assertTrue( entityManager.getOptions().contains( CacheStoreMode.REFRESH ) );
			assertTrue( entityManager.getOptions().contains( FlushModeType.COMMIT ) );

			entityManager.addOption( CacheStoreMode.USE );
			entityManager.addOption( CacheRetrieveMode.USE );

			assertEquals( CacheRetrieveMode.USE, entityManager.getCacheRetrieveMode() );
			assertEquals( CacheStoreMode.USE, entityManager.getCacheStoreMode() );

			assertTrue( entityManager.getOptions().contains( CacheRetrieveMode.USE ) );
			assertTrue( entityManager.getOptions().contains( CacheStoreMode.USE ) );
		}
	}

	@Test
	void entityAgentOptions(EntityManagerFactoryScope scope) {
		try ( var entityAgent = scope.getEntityManagerFactory().createEntityAgent() ) {

			entityAgent.addOption( CacheRetrieveMode.BYPASS );
			entityAgent.addOption( CacheStoreMode.BYPASS );

			assertEquals( CacheRetrieveMode.BYPASS, entityAgent.getCacheRetrieveMode() );
			assertEquals( CacheStoreMode.BYPASS, entityAgent.getCacheStoreMode() );

			assertTrue( entityAgent.getOptions().contains( CacheRetrieveMode.BYPASS ) );
			assertTrue( entityAgent.getOptions().contains( CacheStoreMode.BYPASS ) );

			entityAgent.addOption( CacheStoreMode.USE );
			entityAgent.addOption( CacheRetrieveMode.USE );

			assertEquals( CacheRetrieveMode.USE, entityAgent.getCacheRetrieveMode() );
			assertEquals( CacheStoreMode.USE, entityAgent.getCacheStoreMode() );

			assertTrue( entityAgent.getOptions().contains( CacheRetrieveMode.USE ) );
			assertTrue( entityAgent.getOptions().contains( CacheStoreMode.USE ) );
		}
	}

	@Test
	void entityAgentCreationOptions(EntityManagerFactoryScope scope) {
		try ( var entityAgent = scope.getEntityManagerFactory()
				.createEntityAgent( CacheRetrieveMode.BYPASS, CacheStoreMode.BYPASS ) ) {

			assertEquals( CacheRetrieveMode.BYPASS, entityAgent.getCacheRetrieveMode() );
			assertEquals( CacheStoreMode.BYPASS, entityAgent.getCacheStoreMode() );

			assertTrue( entityAgent.getOptions().contains( CacheRetrieveMode.BYPASS ) );
			assertTrue( entityAgent.getOptions().contains( CacheStoreMode.BYPASS ) );
		}
	}

	@Test
	void typedQueryOptions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final var query =
					entityManager.createQuery( "select b from Book b", Book.class )
							.addOption( Timeout.milliseconds( 1234 ) )
							.addOption( QueryFlushMode.NO_FLUSH )
							.addOption( CacheRetrieveMode.BYPASS )
							.addOption( CacheStoreMode.BYPASS )
							.addOption( LockModeType.PESSIMISTIC_READ )
							.addOption( PessimisticLockScope.EXTENDED );

			assertEquals( 1234, query.getTimeout() );
			assertEquals( QueryFlushMode.NO_FLUSH, query.getQueryFlushMode() );
			assertEquals( CacheRetrieveMode.BYPASS, query.getCacheRetrieveMode() );
			assertEquals( CacheStoreMode.BYPASS, query.getCacheStoreMode() );
			assertEquals( LockModeType.PESSIMISTIC_READ, query.getLockMode() );
			assertEquals( PessimisticLockScope.EXTENDED, query.getLockScope() );

			assertTrue( query.getOptions().contains( Timeout.milliseconds( 1234 ) ) );
			assertTrue( query.getOptions().contains( QueryFlushMode.NO_FLUSH ) );
			assertTrue( query.getOptions().contains( CacheRetrieveMode.BYPASS ) );
			assertTrue( query.getOptions().contains( CacheStoreMode.BYPASS ) );
			assertTrue( query.getOptions().contains( LockModeType.PESSIMISTIC_READ ) );
			assertTrue( query.getOptions().contains( PessimisticLockScope.EXTENDED ) );
		} );
	}

	@Test
	void statementOptions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final var statement =
					entityManager.createStatement( "delete from Book" )
							.addOption( Timeout.milliseconds( 2345 ) )
							.addOption( QueryFlushMode.NO_FLUSH );

			assertEquals( 2345, statement.getTimeout() );
			assertEquals( QueryFlushMode.NO_FLUSH, statement.getQueryFlushMode() );

			assertTrue( statement.getOptions().contains( Timeout.milliseconds( 2345 ) ) );
			assertTrue( statement.getOptions().contains( QueryFlushMode.NO_FLUSH ) );
		} );
	}

	@Test
	void storedProcedureOptions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final var query =
					entityManager.createStoredProcedureQuery( "does_not_execute" )
							.addOption( Timeout.milliseconds( 3456 ) )
							.addOption( QueryFlushMode.NO_FLUSH );

			assertEquals( 3456, query.getTimeout() );
			assertEquals( QueryFlushMode.NO_FLUSH, query.getQueryFlushMode() );

			assertTrue( query.getOptions().contains( Timeout.milliseconds( 3456 ) ) );
			assertTrue( query.getOptions().contains( QueryFlushMode.NO_FLUSH ) );
		} );
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private Long id;
	}
}
