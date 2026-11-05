package org.hibernate.orm.test.locking;

import java.util.stream.Stream;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.lock.internal.OptimisticEntityLockException;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.LockMode.OPTIMISTIC;
import static org.hibernate.LockMode.OPTIMISTIC_FORCE_INCREMENT;
import static org.junit.jupiter.api.Assertions.*;

/// Deferred optimistic work follows the current entry of its original object,
/// across ordinary mutations, persistence-context transitions and completion.
///
/// @author Steve Ebersole
@DomainModel(overrideCacheStrategy = false, annotatedClasses = OptimisticLockLifecycleTest.Item.class)
@SessionFactory(useCollectingStatementObserver = true, generateStatistics = true)
@ExtendWith(FollowOnLockingTestSupport.ReadCommittedEligibility.class)
@ParameterizedClass(name = "queue={0}")
@ValueSource(strings = { "graph", "legacy" })
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@JiraKey("HHH-21035")
@Tag("db-locking")
class OptimisticLockLifecycleTest implements ServiceRegistryProducer {
	private final String queue;

	OptimisticLockLifecycleTest(String queue) { this.queue = queue; }

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder.applySetting( "hibernate.flush.queue.type", queue )
				.applySetting( "hibernate.connection.isolation", 2 )
				.applySetting( "hibernate.cache.use_second_level_cache", true )
				.applySetting( "hibernate.cache.region.factory_class", "org.hibernate.testing.cache.CachingRegionFactory" )
				.applySetting( "hibernate.use_sql_comments", false ).build();
	}

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		assertEquals( java.sql.Connection.TRANSACTION_READ_COMMITTED, scope.getSessionFactory().getJdbcServices()
				.getJdbcEnvironment().getJdbcMetadata().getTransactionIsolation(), "Lifecycle factory baseline" );
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			session.persist( new Item( 1 ) );
			session.persist( new Item( 2 ) );
			session.persist( new Item( 3 ) );
		} );
		scope.inTransaction( session -> {
			assertEquals( 0, session.find( Item.class, 1 ).revision );
			assertEquals( 0, session.find( Item.class, 2 ).revision );
			assertEquals( 0, session.find( Item.class, 3 ).revision );
		} );
		assertTrue( scope.getSessionFactory().getCache().containsEntity( Item.class, 1 ) );
		scope.getCollectingStatementObserver().clear();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	enum Transition { FLUSH, REPEAT, REFRESH, REMOVE, REMOVE_FLUSH, DETACH, CLEAR, MERGE, REPLACE, MERGE_REQUEST, READ_ONLY, ROLLBACK }
	enum Source { LOCK, FIND, QUERY_NEW, QUERY_MANAGED }

	static Stream<Arguments> transitions() {
		return Stream.of( OPTIMISTIC, OPTIMISTIC_FORCE_INCREMENT ).flatMap( mode ->
				Stream.of( Transition.values() ).map( transition -> Arguments.of( mode, transition ) ) );
	}

	static Stream<Arguments> sources() {
		return Stream.of( OPTIMISTIC, OPTIMISTIC_FORCE_INCREMENT ).flatMap( mode ->
				Stream.of( Source.values() ).map( source -> Arguments.of( mode, source ) ) );
	}

	@ParameterizedTest
	@MethodSource("sources")
	void requestsDeferWorkUntilCompletion(LockMode mode, Source source, SessionFactoryScope scope) {
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				final Item item;
				if ( source == Source.LOCK ) {
					item = session.find( Item.class, 1 );
					session.lock( item, mode );
				}
				else if ( source == Source.FIND ) {
					item = session.find( Item.class, 1, mode );
				}
				else {
					final var managed = source == Source.QUERY_MANAGED ? session.find( Item.class, 1 ) : null;
					item = session.createSelectionQuery( "from LifecycleItem where id=1", Item.class )
							.setHibernateLockMode( mode ).getSingleResult();
					if ( managed != null ) assertSame( managed, item );
				}
				assertEquals( 0, item.revision );
				assertTrue( session.getTransactionCompletionCallbacksImplementor().hasBeforeCompletionCallbacks() );
				scope.getCollectingStatementObserver().clear();
				session.flush();
				assertTrue( scope.getCollectingStatementObserver().getSqlQueries().isEmpty(), "Flush must not execute deferred work" );
				transaction.commit();
				assertCompletionSql( scope, mode, true );
				assertEquals( mode == OPTIMISTIC ? 0 : 1, item.revision );
			}
			finally { if ( transaction.isActive() ) transaction.rollback(); }
		}
		assertStored( scope, 1, mode == OPTIMISTIC ? 0 : 1, 0 );
	}

	@ParameterizedTest
	@MethodSource("transitions")
	void persistenceContextTransitions(LockMode mode, Transition transition, SessionFactoryScope scope) {
		final boolean removed = transition == Transition.REMOVE || transition == Transition.REMOVE_FLUSH;
		final boolean skipped = removed || transition == Transition.DETACH || transition == Transition.CLEAR
				|| transition == Transition.MERGE || transition == Transition.REPLACE || transition == Transition.ROLLBACK;
		final int expected = (transition == Transition.FLUSH ? 1 : 0) + (!skipped && mode == OPTIMISTIC_FORCE_INCREMENT ? 1 : 0);
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				final var original = session.find( Item.class, 1 );
				session.lock( original, mode );
				assertEquals( 0, original.revision );
				switch ( transition ) {
					case FLUSH -> {
						original.payload = 7;
						session.flush();
						assertEquals( 1, original.revision );
						assertTrue( session.getTransactionCompletionCallbacksImplementor().hasBeforeCompletionCallbacks() );
					}
					case REPEAT -> { session.lock( original, mode ); session.lock( original, LockMode.NONE ); }
					case REFRESH -> session.refresh( original, mode );
					case REMOVE -> session.remove( original );
					case REMOVE_FLUSH -> { session.remove( original ); session.flush(); }
					case DETACH -> session.detach( original );
					case CLEAR -> session.clear();
					case REPLACE -> { session.detach( original ); assertNotSame( original, session.find( Item.class, 1 ) ); }
					case MERGE, MERGE_REQUEST -> {
						session.detach( original );
						final var replacement = session.merge( original );
						assertNotSame( original, replacement );
						if ( transition == Transition.MERGE_REQUEST ) session.lock( replacement, mode );
					}
					case READ_ONLY -> session.setReadOnly( original, true );
				}
				scope.getCollectingStatementObserver().clear();
				if ( transition == Transition.ROLLBACK ) transaction.rollback(); else transaction.commit();
				assertCompletionSql( scope, mode, !skipped );
				assertFalse( session.getTransactionCompletionCallbacksImplementor().hasBeforeCompletionCallbacks() );
				if ( transition == Transition.READ_ONLY ) {
					assertNull( session.getPersistenceContext().getEntry( original ).getLoadedState() );
				}
				// A healthy session's next transaction must not replay completion work.
				scope.getCollectingStatementObserver().clear();
				session.beginTransaction().commit();
				assertTrue( scope.getCollectingStatementObserver().getSqlQueries().isEmpty() );
			}
			finally { if ( transaction.isActive() ) transaction.rollback(); }
		}
		assertStored( scope, 1, removed ? null : expected, transition == Transition.FLUSH ? 7 : 0 );
	}

	static Stream<Arguments> conflictingChanges() {
		return Stream.of( OPTIMISTIC, OPTIMISTIC_FORCE_INCREMENT ).flatMap( mode ->
				Stream.of( true, false ).map( delete -> Arguments.of( mode, delete ) ) );
	}

	@ParameterizedTest
	@MethodSource("conflictingChanges")
	void deferredWorkDetectsCommittedChange(LockMode mode, boolean delete, SessionFactoryScope scope) {
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				final var item = session.find( Item.class, 1 );
				session.lock( item, mode );
				change( scope, 1, delete );
				assertOptimisticFailure( assertThrows( RuntimeException.class, transaction::commit ) );
			}
			finally { if ( transaction.isActive() ) transaction.rollback(); }
			assertFalse( session.getTransactionCompletionCallbacksImplementor().hasBeforeCompletionCallbacks() );
		}
		assertStored( scope, 1, delete ? null : 1, delete ? 0 : 9 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void laterValidationFailureRollsBackEarlierIncrementAndCacheWork(boolean delete, SessionFactoryScope scope) {
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				final var incremented = session.find( Item.class, 1 );
				session.lock( incremented, OPTIMISTIC_FORCE_INCREMENT );
				session.lock( session.find( Item.class, 2 ), OPTIMISTIC );
				final var pending = session.find( Item.class, 3 );
				session.lock( pending, OPTIMISTIC_FORCE_INCREMENT );
				change( scope, 2, delete );
				scope.getCollectingStatementObserver().clear();
				assertOptimisticFailure( assertThrows( RuntimeException.class, transaction::commit ) );
				assertEquals( 1, incremented.revision, "Earlier callback ran; rollback need not restore the Java version" );
				assertEquals( 0, pending.revision, "Later callback must not run after failed validation" );
				assertEquals( 1, updates( scope ) );
			}
			finally { if ( transaction.isActive() ) transaction.rollback(); }
			assertFalse( session.getTransactionCompletionCallbacksImplementor().hasBeforeCompletionCallbacks() );
		}
		assertStored( scope, 1, 0, 0 );
		assertStored( scope, 2, delete ? null : 1, delete ? 0 : 9 );
		assertStored( scope, 3, 0, 0 );
		// Cache cleanup must also permit a subsequent ordinary update.
		scope.inTransaction( session -> session.find( Item.class, 1 ).payload = 5 );
		assertStored( scope, 1, 1, 5 );
		final var statistics = scope.getSessionFactory().getStatistics();
		final long hits = statistics.getSecondLevelCacheHitCount();
		scope.inTransaction( session -> assertEquals( 1, session.find( Item.class, 1 ).revision ) );
		assertEquals( hits + 1, statistics.getSecondLevelCacheHitCount(), "Cache remains usable after rollback and a new update" );
	}

	private static void change(SessionFactoryScope scope, int id, boolean delete) {
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var item = session.find( Item.class, id );
			if ( delete ) session.remove( item ); else item.payload = 9;
		} );
	}

	private static void assertOptimisticFailure(Throwable failure) {
		for ( var cause = failure; cause != null; cause = cause.getCause() ) {
			if ( cause instanceof OptimisticEntityLockException || cause instanceof StaleObjectStateException
					|| cause instanceof jakarta.persistence.OptimisticLockException ) return;
		}
		throw new AssertionError( "Expected optimistic conflict", failure );
	}

	private static long updates(SessionFactoryScope scope) {
		return scope.getCollectingStatementObserver().getSqlQueries().stream()
				.filter( sql -> sql.startsWith( "update lifecycle_item " ) ).count();
	}

	private static void assertCompletionSql(SessionFactoryScope scope, LockMode mode, boolean scheduled) {
		final var persister = (AbstractEntityPersister) scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Item.class );
		final long checks = scope.getCollectingStatementObserver().getSqlQueries().stream()
				.filter( sql -> sql.equals( persister.getVersionSelectString() ) ).count();
		assertEquals( scheduled && mode == OPTIMISTIC ? 1 : 0, checks, "Deferred version reads" );
		assertEquals( scheduled && mode == OPTIMISTIC_FORCE_INCREMENT ? 1 : 0, updates( scope ), "Deferred increments" );
	}

	private static void assertStored(SessionFactoryScope scope, int id, Integer revision, int payload) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, id ); // cache-visible state first
			if ( revision == null ) assertNull( item );
			else { assertNotNull( item ); assertEquals( revision.intValue(), item.revision ); assertEquals( payload, item.payload ); }
			final var rows = session.createSelectionQuery(
					"select revision, payload from LifecycleItem where id=:id", Object[].class ).setParameter( "id", id ).getResultList();
			if ( revision == null ) assertTrue( rows.isEmpty() );
			else { assertEquals( 1, rows.size() ); assertArrayEquals( new Object[] { revision, payload }, rows.get( 0 ) ); }
		} );
	}

	/// Versioned cache entry used to distinguish ordinary mutation from explicit work.
	/// @author Steve Ebersole
	@Entity(name = "LifecycleItem")
	@Table(name = "lifecycle_item")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Item {
		@Id int id;
		@Version int revision;
		int payload;
		Item() { }
		Item(int id) { this.id = id; }
	}
}
