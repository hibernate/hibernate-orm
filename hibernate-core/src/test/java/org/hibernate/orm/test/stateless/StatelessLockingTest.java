/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static jakarta.persistence.LockModeType.OPTIMISTIC;
import static jakarta.persistence.LockModeType.OPTIMISTIC_FORCE_INCREMENT;
import static jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT;
import static jakarta.persistence.LockModeType.PESSIMISTIC_READ;
import static jakarta.persistence.LockModeType.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Jpa(annotatedClasses = StatelessLockingTest.Lockable.class,
		useCollectingStatementInspector = true,
		properties = @Setting(name = AvailableSettings.CACHE_REGION_FACTORY,
				value = "org.hibernate.testing.cache.CachingRegionFactory"))
class StatelessLockingTest {
	private static final String QUERY = "from StatelessLockable order by id";

	@BeforeEach
	void prepare(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new Lockable( 1, "first" ) );
			em.persist( new Lockable( 2, "second" ) );
		} );
	}

	@AfterEach
	void cleanup(EntityManagerFactoryScope scope) {
		scope.dropData();
		scope.getEntityManagerFactory().getCache().evictAll();
	}

	static Stream<Arguments> forceIncrementOperations() {
		return Stream.of( WRITE, OPTIMISTIC_FORCE_INCREMENT, PESSIMISTIC_FORCE_INCREMENT )
				.flatMap( mode -> Stream.of( Operation.values() ).map( operation -> Arguments.of( mode, operation ) ) );
	}

	@ParameterizedTest
	@MethodSource("forceIncrementOperations")
	void testImmediateForceIncrement(LockModeType mode, Operation operation, EntityManagerFactoryScope scope) {
		try ( var agent = scope.getEntityManagerFactory().createEntityAgent() ) {
			final var transaction = agent.getTransaction();
			transaction.begin();
			try {
				final var entity = operation.read( agent, mode );
				assertEquals( 1, entity.version );
				entity.name = "changed only in memory";
				transaction.commit();
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
		assertStoredState( scope, "first", 1 );
	}

	@ParameterizedTest
	@EnumSource(value = LockModeType.class, names = { "WRITE", "OPTIMISTIC_FORCE_INCREMENT", "PESSIMISTIC_FORCE_INCREMENT" })
	void testForceIncrementThenUpdate(LockModeType mode, EntityManagerFactoryScope scope) {
		try ( var agent = scope.getEntityManagerFactory().createEntityAgent() ) {
			final var transaction = agent.getTransaction();
			transaction.begin();
			try {
				final var entity = agent.find( Lockable.class, 1, mode );
				assertEquals( 1, entity.version );
				entity.name = "updated";
				agent.update( entity );
				assertEquals( 2, entity.version );
				transaction.commit();
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
		assertStoredState( scope, "updated", 2 );
	}

	@ParameterizedTest
	@EnumSource(value = LockModeType.class, names = { "WRITE", "OPTIMISTIC_FORCE_INCREMENT", "PESSIMISTIC_FORCE_INCREMENT" })
	void testForceIncrementRollback(LockModeType mode, EntityManagerFactoryScope scope) {
		try ( var agent = scope.getEntityManagerFactory().createEntityAgent() ) {
			final var transaction = agent.getTransaction();
			transaction.begin();
			try {
				assertEquals( 1, agent.find( Lockable.class, 1, mode ).version );
			}
			finally {
				transaction.rollback();
			}
		}
		assertStoredState( scope, "first", 0 );
	}

	@ParameterizedTest
	@EnumSource(value = LockModeType.class, names = { "WRITE", "OPTIMISTIC_FORCE_INCREMENT", "PESSIMISTIC_FORCE_INCREMENT" })
	void testDuplicateQueryResults(LockModeType mode, EntityManagerFactoryScope scope) {
		try ( var agent = scope.getEntityManagerFactory().createEntityAgent() ) {
			final var transaction = agent.getTransaction();
			transaction.begin();
			try {
				final var results = agent.createQuery(
						"select l, other.id from StatelessLockable l cross join StatelessLockable other order by l.id, other.id",
						Object[].class ).setLockMode( mode ).getResultList();
				assertEquals( 4, results.size() );
				assertSame( results.get( 0 )[0], results.get( 1 )[0] );
				assertSame( results.get( 2 )[0], results.get( 3 )[0] );
				for ( var result : results ) {
					assertEquals( 1, ((Lockable) result[0]).version );
				}
				transaction.commit();
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
		assertStoredState( scope, "first", 1 );
	}

	@ParameterizedTest
	@EnumSource(value = LockModeType.class, names = { "READ", "OPTIMISTIC" })
	void testOptimisticQueryUsesImmediateLock(LockModeType mode, EntityManagerFactoryScope scope) {
		final var originalSql = statefulQuerySql( scope );
		List<String> optimisticSql;
		try ( var agent = scope.getEntityManagerFactory().createEntityAgent() ) {
			final var transaction = agent.getTransaction();
			transaction.begin();
			try {
				final var query = agent.createQuery( QUERY, Lockable.class ).setLockMode( mode );
				final var inspector = scope.getCollectingStatementInspector();
				inspector.clear();
				query.getResultList();
				optimisticSql = List.copyOf( inspector.getSqlQueries() );
				assertEquals( OPTIMISTIC, query.getLockMode() );
				inspector.clear();
				query.setLockMode( PESSIMISTIC_READ ).getResultList();
				assertEquals( inspector.getSqlQueries(), optimisticSql );
				transaction.commit();
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
		assertEquals( originalSql, statefulQuerySql( scope ) );
		assertStoredState( scope, "first", 0 );
	}

	private List<String> statefulQuerySql(EntityManagerFactoryScope scope) {
		return scope.fromTransaction( em -> {
			final var inspector = scope.getCollectingStatementInspector();
			inspector.clear();
			em.createQuery( QUERY, Lockable.class ).setLockMode( OPTIMISTIC ).getResultList();
			return List.copyOf( inspector.getSqlQueries() );
		} );
	}

	private void assertStoredState(EntityManagerFactoryScope scope, String name, int version) {
		scope.inTransaction( em -> {
			// Verify both cache-visible entity state and the database row.
			final var entity = em.find( Lockable.class, 1 );
			assertEquals( name, entity.name );
			assertEquals( version, entity.version );
			final var row = em.createQuery(
					"select name, version from StatelessLockable where id = 1", Object[].class ).getSingleResult();
			assertEquals( name, row[0] );
			assertEquals( version, row[1] );
		} );
	}

	enum Operation {
		FIND, REFRESH, MULTIPLE, QUERY, STREAM;

		Lockable read(EntityAgent agent, LockModeType mode) {
			return switch ( this ) {
				case FIND -> agent.find( Lockable.class, 1, mode );
				case REFRESH -> {
					final var entity = new Lockable( 1, "old" );
					agent.refresh( entity, mode );
					yield entity;
				}
				case MULTIPLE -> agent.findMultiple( Lockable.class, List.of( 1, 2 ), mode ).get( 0 );
				case QUERY -> agent.createQuery( StatelessLockingTest.QUERY, Lockable.class ).setLockMode( mode ).getResultList().get( 0 );
				case STREAM -> {
					try ( var stream = agent.createQuery( StatelessLockingTest.QUERY, Lockable.class ).setLockMode( mode ).getResultStream() ) {
						yield stream.toList().get( 0 );
					}
				}
			};
		}
	}

	@Entity(name = "StatelessLockable")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Lockable {
		@Id
		int id;
		@Version
		int version;
		String name;

		Lockable() {
		}

		Lockable(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
