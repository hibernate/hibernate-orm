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
import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FindOption;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStatement;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.StatementReference;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
	void nullVarargOptionArrays(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().runInTransaction( entityManager ->
				entityManager.persist( new Book( 1L, "before" ) )
		);

		try ( var entityManager = scope.getEntityManagerFactory()
				.createEntityManager( (EntityManager.CreationOption[]) null ) ) {
			entityManager.getTransaction().begin();
			final var book = entityManager.find( Book.class, 1L, (FindOption[]) null );
			final var graph = entityManager.createEntityGraph( Book.class );

			assertEquals( "before", book.title );
			assertEquals( book, entityManager.find( graph, 1L, (FindOption[]) null ) );
			assertEquals( book, entityManager.get( Book.class, 1L, (FindOption[]) null ) );
			assertEquals( book, entityManager.get( graph, 1L, (FindOption[]) null ) );
			assertEquals( List.of( book ), entityManager.findMultiple( Book.class, List.of( 1L ), (FindOption[]) null ) );
			assertEquals( List.of( book ), entityManager.findMultiple( graph, List.of( 1L ), (FindOption[]) null ) );
			assertEquals( List.of( book ), entityManager.getMultiple( Book.class, List.of( 1L ), (FindOption[]) null ) );
			assertEquals( List.of( book ), entityManager.getMultiple( graph, List.of( 1L ), (FindOption[]) null ) );

			entityManager.lock( book, LockModeType.NONE, (LockOption[]) null );
			entityManager.createStatement( "update Book b set b.title = 'after' where b.id = :id" )
					.setParameter( "id", 1L )
					.execute();
			entityManager.refresh( book, (RefreshOption[]) null );
			assertEquals( "after", book.title );
			entityManager.getTransaction().commit();
		}

		try ( var entityAgent = scope.getEntityManagerFactory()
				.createEntityAgent( (EntityAgent.CreationOption[]) null ) ) {
			final var book = entityAgent.find( Book.class, 1L, (FindOption[]) null );
			final var graph = entityAgent.createEntityGraph( Book.class );

			assertEquals( "after", book.title );
			assertEquals( "after", entityAgent.find( graph, 1L, (FindOption[]) null ).title );
			assertEquals( "after", entityAgent.get( Book.class, 1L, (FindOption[]) null ).title );
			assertEquals( "after", entityAgent.get( graph, 1L, (FindOption[]) null ).title );
			assertEquals( 1, entityAgent.findMultiple( Book.class, List.of( 1L ), (FindOption[]) null ).size() );
			assertEquals( 1, entityAgent.findMultiple( graph, List.of( 1L ), (FindOption[]) null ).size() );
			assertEquals( 1, entityAgent.getMultiple( Book.class, List.of( 1L ), (FindOption[]) null ).size() );
			assertEquals( 1, entityAgent.getMultiple( graph, List.of( 1L ), (FindOption[]) null ).size() );
		}
	}

	@Test
	void nullHintMaps(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().runInTransaction( entityManager ->
				entityManager.persist( new Book( 2L, "before" ) )
		);

		try ( var entityManager = scope.getEntityManagerFactory()
				.createEntityManager( (Map<?, ?>) null ) ) {
			entityManager.getTransaction().begin();
			final var book = entityManager.find( Book.class, 2L, (Map<String, Object>) null );

			assertEquals( "before", book.title );
			assertEquals( book, entityManager.find( Book.class, 2L, LockModeType.NONE, (Map<String, Object>) null ) );
			entityManager.lock( book, LockModeType.NONE, (Map<String, Object>) null );

			entityManager.createStatement( "update Book b set b.title = 'after' where b.id = :id" )
					.setParameter( "id", 2L )
					.execute();
			entityManager.refresh( book, (Map<String, Object>) null );
			assertEquals( "after", book.title );

			entityManager.createStatement( "update Book b set b.title = 'again' where b.id = :id" )
					.setParameter( "id", 2L )
					.execute();
			entityManager.refresh( book, LockModeType.NONE, (Map<String, Object>) null );
			assertEquals( "again", book.title );
			entityManager.getTransaction().commit();
		}

		try ( var entityAgent = scope.getEntityManagerFactory()
				.createEntityAgent( (Map<?, ?>) null ) ) {
			Book book = entityAgent.find( Book.class, 2L );
			assertNotNull( book );
			assertEquals( "again", book.title );
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

		private String title;

		Book() {
		}

		Book(Long id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
