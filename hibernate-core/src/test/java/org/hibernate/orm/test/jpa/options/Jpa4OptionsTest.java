/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.options;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStatement;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.StatementReference;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
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

	@Test
	void typedQueryReferenceOptions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final var query = entityManager.createQuery(
					new TypedReference<>(
							"Book.referenceOptionsQuery",
							Book.class,
							Set.of( QueryFlushMode.FLUSH, Timeout.milliseconds( 4567 ) )
					)
			);

			assertEquals( QueryFlushMode.FLUSH, query.getQueryFlushMode() );
			assertEquals( 4567, query.getTimeout() );
			assertTrue( query.getOptions().contains( QueryFlushMode.FLUSH ) );
			assertTrue( query.getOptions().contains( Timeout.milliseconds( 4567 ) ) );
		} );
	}

	@Test
	void statementReferenceOptions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final var statement = entityManager.createStatement(
					new StatementRef(
							"Book.referenceOptionsStatement",
							Set.of( QueryFlushMode.NO_FLUSH, Timeout.milliseconds( 5678 ) )
					)
			);

			assertEquals( QueryFlushMode.NO_FLUSH, statement.getQueryFlushMode() );
			assertEquals( 5678, statement.getTimeout() );
			assertTrue( statement.getOptions().contains( QueryFlushMode.NO_FLUSH ) );
			assertTrue( statement.getOptions().contains( Timeout.milliseconds( 5678 ) ) );
		} );
	}

	private record TypedReference<R>(
			String name,
			Class<R> resultType,
			Set<TypedQuery.Option> options) implements TypedQueryReference<R> {
		@Override
		public String getName() {
			return name;
		}

		@Override
		public Class<? extends R> getResultType() {
			return resultType;
		}

		@Override
		public List<Class<?>> getParameterTypes() {
			return null;
		}

		@Override
		public List<String> getParameterNames() {
			return null;
		}

		@Override
		public List<Object> getArguments() {
			return null;
		}

		@Override
		public Map<String, Object> getHints() {
			return Map.of();
		}

		@Override
		public Set<TypedQuery.Option> getOptions() {
			return options;
		}

		@Override
		public String getEntityGraphName() {
			return null;
		}
	}

	private record StatementRef(String name, Set<jakarta.persistence.Statement.Option> options)
			implements StatementReference {
		@Override
		public String getName() {
			return name;
		}

		@Override
		public List<Class<?>> getParameterTypes() {
			return null;
		}

		@Override
		public List<String> getParameterNames() {
			return null;
		}

		@Override
		public List<Object> getArguments() {
			return null;
		}

		@Override
		public Map<String, Object> getHints() {
			return Map.of();
		}

		@Override
		public Set<jakarta.persistence.Statement.Option> getOptions() {
			return options;
		}
	}

	@Entity(name = "Book")
	@NamedQuery(name = "Book.referenceOptionsQuery", query = "select b from Book b")
	@NamedStatement(name = "Book.referenceOptionsStatement", statement = "delete from Book")
	public static class Book {
		@Id
		private Long id;
	}
}
