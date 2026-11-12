package org.hibernate.orm.test.locking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.HibernateException;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.LockMode.PESSIMISTIC_FORCE_INCREMENT;

/// Direct force increments complete collection acquisition before changing versions
/// or presenting completed results to list transformers and callers.
///
/// @author Steve Ebersole
@DomainModel(overrideCacheStrategy = false, annotatedClasses = {
		DirectForceIncrementOrderingTest.Item.class, DirectForceIncrementOrderingTest.Other.class,
		DirectForceIncrementOrderingTest.Plain.class
})
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true")
})
@ExtendWith(FollowOnLockingTestSupport.ReadCommittedEligibility.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
class DirectForceIncrementOrderingTest {
	private static final ThreadLocal<Trace> TRACE = new ThreadLocal<>();

	@BeforeEach
	void seed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1 ) );
			session.persist( new Item( 2 ) );
			session.persist( new Other() );
		} );
		TRACE.set( new Trace() );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		TRACE.remove();
		scope.dropData();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	static Stream<Arguments> loads() {
		return Stream.of( Path.values() ).flatMap( path -> Stream.of( false, true ).flatMap( managed ->
				Stream.of( false, true ).map( readOnly -> Arguments.of( path, managed, readOnly ) ) ) );
	}

	static Stream<Arguments> acquisitionFailures() {
		return loads().flatMap( args -> Stream.of( 1, 2 ).map( failureIndex ->
				Arguments.of( args.get()[0], args.get()[1], args.get()[2], failureIndex ) ) );
	}

	@ParameterizedTest
	@MethodSource("loads")
	void acquisitionPrecedesIncrement(Path path, boolean managed, boolean readOnly, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( readOnly );
			if ( managed ) {
				session.find( Item.class, 1 );
			}
			TRACE.get().loaded.clear();
			scope.getCollectingStatementObserver().clear();
			final var result = path.read( session );
			assertThat( result.version ).isEqualTo( 1 );
			final var entry = session.getPersistenceContextInternal().getEntry( result );
			assertThat( entry.getVersion() ).isEqualTo( 1 );
			assertThat( entry.isReadOnly() ).isEqualTo( readOnly );
			if ( readOnly ) {
				assertThat( entry.getLoadedState() ).isNull();
			}
			else {
				assertThat( entry.getLoadedState()[entry.getPersister().getVersionPropertyIndex()] ).isEqualTo( 1 );
			}
			assertThat( session.getCurrentLockMode( result ) ).isEqualTo( PESSIMISTIC_FORCE_INCREMENT );
			assertThat( TRACE.get().loaded ).hasSize( managed ? 0 : 1 );
			assertOrdering( scope );
		} );
		assertStoredVersion( scope, 1 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void listsAcquireEveryCollectionBeforeAnyIncrementAndTransformation(boolean managed, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			if ( managed ) {
				session.find( Item.class, 1 );
			}
			TRACE.get().loaded.clear();
			scope.getCollectingStatementObserver().clear();
			final var query = session.createQuery( "from DirectForceItem order by id", Item.class )
					.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
					.setFollowOnStrategy( Locking.FollowOn.DISALLOW );
			final var transformed = new AtomicInteger();
			final org.hibernate.query.ResultListTransformer<Item> transformer = items -> {
				transformed.incrementAndGet();
				assertOrdering( scope );
				assertThat( items ).allSatisfy( item -> assertThat( item.version ).isEqualTo( 1 ) );
				// Copying the version here detects accidentally transforming pending state.
				return items.stream().map( item -> {
					final var copy = new Item( item.id );
					copy.version = item.version;
					return copy;
				} ).toList();
			};
			query.setResultListTransformer( transformer );
			assertThat( query.getResultList() ).hasSize( 2 ).allSatisfy( item -> assertThat( item.version ).isEqualTo( 1 ) );
			assertThat( transformed ).hasValue( 1 );
			assertThat( query.getQueryOptions().getResultListTransformer() ).isSameAs( transformer );
		} );
	}

	@ParameterizedTest
	@MethodSource("acquisitionFailures")
	void acquisitionFailureDoesNotIncrement(Path path, boolean managed, boolean readOnly, int failureIndex, SessionFactoryScope scope) {
		final var failure = new HibernateException( "collection acquisition failed" );
		final var acquisitions = new AtomicInteger();
		try ( var session = scope.getSessionFactory().withOptions().statementInspector( (StatementInspector) sql -> {
			if ( (sql.contains( "from direct_force_labels " ) || sql.contains( "from direct_force_tags " ))
					&& acquisitions.incrementAndGet() == failureIndex ) {
				throw failure;
			}
			return sql;
		} ).openSession() ) {
			final var tx = session.beginTransaction();
			try {
				session.setDefaultReadOnly( readOnly );
				if ( managed ) {
					session.find( Item.class, 1 );
				}
				scope.getCollectingStatementObserver().clear();
				assertThatThrownBy( () -> path.read( session ) ).isSameAs( failure );
				assertThat( sql( scope ) ).noneMatch( DirectForceIncrementOrderingTest::isIncrement );
				assertThat( TRACE.get().loaded ).allSatisfy( item -> {
					assertThat( item.version ).isZero();
					assertThat( session.getPersistenceContextInternal().getEntry( item ).getVersion() ).isEqualTo( 0 );
				} );
			}
			finally {
				tx.rollback();
			}
		}
		assertStoredVersion( scope, 0 );
	}

	@Test
	void staleManagedVersionIsStillRejectedBeforeIncrement(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireDeletionRace( scope );
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var tx = session.beginTransaction();
			try {
				final var item = session.find( Item.class, 1 );
				scope.inTransaction( writer -> writer.createNativeMutationQuery( "update direct_force_item set version=version+1 where id=1" ).executeUpdate() );
				scope.getCollectingStatementObserver().clear();
				assertThatThrownBy( () -> Path.LIST.read( session ) ).isInstanceOfAny(
						jakarta.persistence.OptimisticLockException.class, org.hibernate.StaleObjectStateException.class );
				assertThat( item.version ).isZero();
				assertThat( sql( scope ) ).noneMatch( DirectForceIncrementOrderingTest::isIncrement );
			}
			finally { tx.rollback(); }
		}
		assertStoredVersion( scope, 1 );
	}

	@Test
	void provisionalWriteModeDoesNotAcceptUnversionedForceIncrement(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Plain() ) );
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var tx = session.beginTransaction();
			try {
				assertThatThrownBy( () -> session.find( Plain.class, 1, PESSIMISTIC_FORCE_INCREMENT, EXTENDED, Locking.FollowOn.DISALLOW ) )
						.isInstanceOf( HibernateException.class ).hasMessageContaining( "has no version" );
			}
			finally { tx.rollback(); }
		}
	}

	@Test
	void emptyCollectionsAreSuccessfulAcquisition(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = new Item( 3 );
			item.tags.clear(); item.labels.clear();
			session.persist( item );
		} );
		scope.inTransaction( session -> {
			scope.getCollectingStatementObserver().clear();
			assertThat( session.find( Item.class, 3, PESSIMISTIC_FORCE_INCREMENT, EXTENDED, Locking.FollowOn.DISALLOW ).version ).isEqualTo( 1 );
			assertOrdering( scope );
		} );
	}

	@Test
	void failedAcquisitionDoesNotInvokeListTransformer(SessionFactoryScope scope) {
		final var transformed = new AtomicInteger();
		try ( var session = scope.getSessionFactory().withOptions().statementInspector( (StatementInspector) sql -> {
			if ( sql.contains( "from direct_force_tags " ) ) {
				throw new HibernateException( "first collection failed" );
			}
			return sql;
		} ).openSession() ) {
			final var tx = session.beginTransaction();
			try {
				assertThatThrownBy( () -> session.createQuery( "from DirectForceItem", Item.class )
						.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
						.setFollowOnStrategy( Locking.FollowOn.DISALLOW ).setResultListTransformer( items -> {
							transformed.incrementAndGet();
							return items;
						} ).getResultList() ).isInstanceOf( HibernateException.class );
				assertThat( transformed ).hasValue( 0 );
			}
			finally {
				tx.rollback();
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void cursorDoesNotIncrementUnconsumedResults(boolean fetch, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = session.createQuery( "from DirectForceItem i " + (fetch ? "join fetch i.tags " : "") + "order by i.id", Item.class )
					.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
					.setFollowOnStrategy( Locking.FollowOn.DISALLOW );
			scope.getCollectingStatementObserver().clear();
			try ( var unused = query.scroll( ScrollMode.FORWARD_ONLY ) ) {
				assertThat( sql( scope ) ).noneMatch( DirectForceIncrementOrderingTest::isIncrement );
			}
			try ( var cursor = query.scroll( ScrollMode.FORWARD_ONLY ) ) {
				assertThat( cursor.next() ).isTrue();
				assertThat( cursor.get().id ).isEqualTo( 1 );
				assertThat( cursor.get().version ).isEqualTo( 1 );
				assertOrdering( scope );
			}
		} );
		scope.inTransaction( session -> assertThat( session.find( Item.class, 2 ).version ).isZero() );
	}

	@ParameterizedTest
	@CsvSource({ "false, false", "false, true", "true, false", "true, true" })
	void statelessCompletionAndFailure(boolean stream, boolean fail, SessionFactoryScope scope) {
		try ( var owner = scope.getSessionFactory().openStatelessSession(); var blocker = scope.getSessionFactory().openSession() ) {
			final var tx = owner.beginTransaction();
			final var block = blocker.beginTransaction();
			try {
				if ( fail ) {
					org.junit.jupiter.api.Assumptions.assumeTrue( new DialectFeatureChecks.SupportsWait().apply( scope.getSessionFactory().getJdbcServices().getDialect() ) );
					org.junit.jupiter.api.Assumptions.assumeTrue( new DialectFeatureChecks.SupportsConcurrentTransactions().apply( scope.getSessionFactory().getJdbcServices().getDialect() ) );
					blocker.createNativeMutationQuery( "update direct_force_tags set tags='blocked' where owner_id=1 and tags='one'" ).executeUpdate();
				}
				scope.getCollectingStatementObserver().clear();
				final var query = owner.createQuery( "from DirectForceItem where id=1", Item.class )
						.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
						.setFollowOnStrategy( Locking.FollowOn.DISALLOW )
						.setLockTimeout( fail ? jakarta.persistence.Timeout.seconds( 1 ) : org.hibernate.Timeouts.WAIT_FOREVER );
				final java.util.function.Supplier<Item> read = () -> {
					if ( stream ) {
						try ( var results = query.getResultStream() ) { return results.findFirst().orElseThrow(); }
					}
					return query.getSingleResult();
				};
				if ( fail ) {
					assertThatThrownBy( read::get ).isInstanceOfAny( org.hibernate.exception.LockTimeoutException.class,
							org.hibernate.exception.LockAcquisitionException.class, jakarta.persistence.LockTimeoutException.class,
							jakarta.persistence.PessimisticLockException.class );
					assertThat( sql( scope ) ).noneMatch( DirectForceIncrementOrderingTest::isIncrement );
				}
				else {
					assertThat( read.get().version ).isEqualTo( 1 );
					assertOrdering( scope );
				}
			}
			finally {
				tx.rollback();
				block.rollback();
			}
		}
		assertStoredVersion( scope, 0 );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void allEntityTypesAcquireBeforeEitherIncrement(boolean scroll, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			scope.getCollectingStatementObserver().clear();
			final var query = session.createQuery( "select i,o from DirectForceItem i cross join DirectForceOther o where i.id=1", Object[].class )
					.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
					.setFollowOnStrategy( Locking.FollowOn.DISALLOW );
			final Object[] result;
			if ( scroll ) {
				try ( var cursor = query.scroll( ScrollMode.FORWARD_ONLY ) ) {
					assertThat( cursor.next() ).isTrue();
					result = cursor.get();
				}
			}
			else {
				result = query.getSingleResult();
			}
			assertThat( ((Item) result[0]).version ).isEqualTo( 1 );
			assertThat( ((Other) result[1]).version ).isEqualTo( 1 );
			assertOrdering( scope );
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void duplicateRowsDoNotRepeatIncrement(boolean stream, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = session.createQuery( "select i from DirectForceItem i join i.tags t where i.id=1 order by t", Item.class )
					.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
					.setFollowOnStrategy( Locking.FollowOn.DISALLOW );
			scope.getCollectingStatementObserver().clear();
			final List<Item> results;
			if ( stream ) {
				try ( var values = query.getResultStream() ) { results = values.toList(); }
			}
			else { results = query.getResultList(); }
			assertThat( results ).isNotEmpty().allSatisfy( item -> assertThat( item.version ).isEqualTo( 1 ) );
			assertThat( sql( scope ).stream().filter( DirectForceIncrementOrderingTest::isIncrement ).count() ).isEqualTo( 1 );
		} );
	}

	@Test
	void failedCursorCannotReplayPendingIncrement(SessionFactoryScope scope) {
		try ( var session = scope.getSessionFactory().withOptions().statementInspector( (StatementInspector) sql -> {
			if ( sql.contains( "from direct_force_labels " ) ) { throw new AssertionError( "collection failure" ); }
			return sql;
		} ).openSession() ) {
			final var tx = session.beginTransaction();
			try ( var cursor = session.createQuery( "from DirectForceItem order by id", Item.class )
					.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
					.setFollowOnStrategy( Locking.FollowOn.DISALLOW ).scroll( ScrollMode.FORWARD_ONLY ) ) {
				scope.getCollectingStatementObserver().clear();
				assertThatThrownBy( cursor::next ).isInstanceOf( AssertionError.class );
				assertThatThrownBy( cursor::next ).isInstanceOf( HibernateException.class ).hasMessageContaining( "Cannot resume" );
				assertThat( sql( scope ) ).noneMatch( DirectForceIncrementOrderingTest::isIncrement );
			}
			finally { tx.rollback(); }
		}
	}

	@Test
	void transformerFailureRollsBackCompletedIncrement(SessionFactoryScope scope) {
		final var failure = new HibernateException( "transformer failure" );
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var tx = session.beginTransaction();
			try {
				scope.getCollectingStatementObserver().clear();
				assertThatThrownBy( () -> session.createQuery( "from DirectForceItem where id=1", Item.class )
						.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
						.setFollowOnStrategy( Locking.FollowOn.DISALLOW ).setResultListTransformer( items -> {
							assertThat( items.get( 0 ).version ).isEqualTo( 1 );
							throw failure;
						} ).getResultList() ).isSameAs( failure );
				assertThat( sql( scope ).stream().filter( DirectForceIncrementOrderingTest::isIncrement ).count() ).isEqualTo( 1 );
			}
			finally { tx.rollback(); }
		}
		assertStoredVersion( scope, 0 );
	}

	@Test
	void emptyResultStillTransformsOnce(SessionFactoryScope scope) {
		final var transformed = new AtomicInteger();
		scope.inTransaction( session -> {
			scope.getCollectingStatementObserver().clear();
			assertThat( session.createQuery( "from DirectForceItem where id=99", Item.class )
					.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
					.setFollowOnStrategy( Locking.FollowOn.DISALLOW ).setResultListTransformer( items -> {
						transformed.incrementAndGet(); return items;
					} ).getResultList() ).isEmpty();
			assertThat( transformed ).hasValue( 1 );
			assertThat( sql( scope ) ).noneMatch( DirectForceIncrementOrderingTest::isIncrement );
		} );
	}

	private static void assertOrdering(SessionFactoryScope scope) {
		final var sql = sql( scope );
		final int firstIncrement = java.util.stream.IntStream.range( 0, sql.size() ).filter( i -> isIncrement( sql.get( i ) ) )
				.findFirst().orElseThrow();
		assertThat( sql.subList( firstIncrement, sql.size() ) )
				.noneMatch( statement -> statement.startsWith( "select " ) && statement.contains( "from direct_force_" ) );
		assertThat( sql.subList( 0, firstIncrement ) ).anyMatch( statement -> statement.contains( "from direct_force_tags " ) );
		assertThat( sql.subList( 0, firstIncrement ) ).anyMatch( statement -> statement.contains( "from direct_force_labels " ) );
	}

	private static List<String> sql(SessionFactoryScope scope) {
		return scope.getCollectingStatementObserver().getSqlQueries();
	}

	private static boolean isIncrement(String sql) {
		return sql.startsWith( "update direct_force_item " ) || sql.startsWith( "update direct_force_other " );
	}

	private static void assertStoredVersion(SessionFactoryScope scope, int version) {
		final var trace = TRACE.get();
		TRACE.remove();
		try {
			scope.inTransaction( session -> assertThat( session.find( Item.class, 1 ).version ).isEqualTo( version ) );
			scope.getSessionFactory().getCache().evictAllRegions();
			scope.inTransaction( session -> assertThat( session.find( Item.class, 1 ).version ).isEqualTo( version ) );
		}
		finally {
			TRACE.set( trace );
		}
	}

	enum Path {
		FIND, LIST, STREAM, SCROLL;

		Item read(Session session) {
			if ( this == FIND ) {
				return session.find( Item.class, 1, PESSIMISTIC_FORCE_INCREMENT, EXTENDED, Locking.FollowOn.DISALLOW );
			}
			final var query = session.createQuery( "from DirectForceItem where id=1", Item.class )
					.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
					.setFollowOnStrategy( Locking.FollowOn.DISALLOW );
			if ( this == LIST ) {
				return query.getSingleResult();
			}
			if ( this == STREAM ) {
				try ( var stream = query.getResultStream() ) {
					return stream.findFirst().orElseThrow();
				}
			}
			try ( var cursor = query.scroll( ScrollMode.FORWARD_ONLY ) ) {
				assertThat( cursor.next() ).isTrue();
				return cursor.get();
			}
		}
	}

	static class Trace {
		final List<Item> loaded = new ArrayList<>();
	}

	@Entity(name = "DirectForceItem")
	@Table(name = "direct_force_item")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Item {
		@Id int id;
		@Version int version;
		@ElementCollection
		@CollectionTable(name = "direct_force_tags", joinColumns = @JoinColumn(name = "owner_id"))
		Set<String> tags = new HashSet<>( Set.of( "one", "two" ) );
		@ElementCollection
		@CollectionTable(name = "direct_force_labels", joinColumns = @JoinColumn(name = "owner_id"))
		Set<String> labels = new HashSet<>( Set.of( "label" ) );

		Item() {}
		Item(int id) { this.id = id; }

		@PostLoad
		void loaded() {
			final var trace = TRACE.get();
			if ( trace != null ) {
				trace.loaded.add( this );
				assertThat( version ).isZero();
			}
		}
	}

	@Entity(name = "DirectForceOther")
	@Table(name = "direct_force_other")
	static class Other {
		@Id int id = 1;
		@Version int version;
		@ElementCollection
		@CollectionTable(name = "direct_force_other_tags", joinColumns = @JoinColumn(name = "owner_id"))
		Set<String> tags = new HashSet<>( Set.of( "tag" ) );

		@PostLoad
		void loaded() { assertThat( version ).isZero(); }
	}

	@Entity(name = "DirectForcePlain")
	@Table(name = "direct_force_plain")
	static class Plain {
		@Id int id = 1;
	}
}
