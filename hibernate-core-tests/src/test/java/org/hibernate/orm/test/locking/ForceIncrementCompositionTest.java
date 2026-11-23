package org.hibernate.orm.test.locking;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;
import jakarta.persistence.Version;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.LockMode.OPTIMISTIC_FORCE_INCREMENT;
import static org.hibernate.LockMode.PESSIMISTIC_FORCE_INCREMENT;

/// Preserves independently scheduled optimistic increments across pessimistic acquisition.
///
/// @author Steve Ebersole
@org.junit.jupiter.api.extension.ExtendWith( FollowOnLockingTestSupport.ReadCommittedEligibility.class )
@DomainModel( overrideCacheStrategy = false, annotatedClasses = ForceIncrementCompositionTest.Item.class )
@SessionFactory( useCollectingStatementObserver = true, generateStatistics = true )
@ParameterizedClass( name = "queue={0}" )
@ValueSource( strings = {"graph", "legacy"} )
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
class ForceIncrementCompositionTest implements ServiceRegistryProducer {
	private final String queue;

	ForceIncrementCompositionTest( String queue ) { this.queue = queue; }

	@Override
	public StandardServiceRegistry produceServiceRegistry( StandardServiceRegistryBuilder builder ) {
		return builder.applySetting( "hibernate.flush.queue.type", queue )
				.applySetting( "hibernate.connection.isolation", "2" )
				.applySetting( "hibernate.use_sql_comments", "false" )
				.applySetting( "hibernate.cache.use_second_level_cache", "true" )
				.applySetting( "hibernate.cache.region.factory_class", "org.hibernate.testing.cache.CachingRegionFactory" )
				.build();
	}

	@BeforeEach
	void seed( SessionFactoryScope scope ) {
		scope.inTransaction( s -> s.persist( new Item() ) );
		scope.getCollectingStatementObserver().clear();
	}

	@AfterEach
	void cleanup( SessionFactoryScope scope ) {
		scope.dropData();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	static Stream<Arguments> transitions() {
		return Stream.of( Source.values() ).flatMap( source -> Stream.of( Path.values() ).map( path -> Arguments.of( source, path ) ) );
	}

	@ParameterizedTest( name = "source={0}, path={1}" )
	@MethodSource( "transitions" )
	void optimisticThenPessimistic( Source source, Path path, SessionFactoryScope scope ) {
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var tx = session.beginTransaction();
			try {
				final var item = source.schedule( session );
				assertThat( item.version ).isZero();
				path.acquire( session, item );
				assertThat( item.version ).isEqualTo( 1 );
				assertThat( session.asEventSource().getActionQueue().getTransactionCompletionCallbacks().hasBeforeCompletionCallbacks() ).isTrue();
				assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_FORCE_INCREMENT );
				tx.commit();
				assertThat( item.version ).isEqualTo( 2 );
				assertThat( updates( scope ) ).isEqualTo( 2 );
			} finally { if ( tx.isActive() ) tx.rollback(); }
		}
		assertStored( scope, 2 );
	}

	@ParameterizedTest
	@EnumSource( Path.class )
	void pessimisticThenOptimistic( Path path, SessionFactoryScope scope ) {
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var tx = session.beginTransaction();
			try {
				final var item = session.find( Item.class, 1 );
				path.acquire( session, item );
				session.lock( item, OPTIMISTIC_FORCE_INCREMENT );
				assertThat( session.asEventSource().getActionQueue().getTransactionCompletionCallbacks().hasBeforeCompletionCallbacks() ).isFalse();
				tx.commit();
				assertThat( item.version ).isEqualTo( 1 );
				assertThat( updates( scope ) ).isEqualTo( 1 );
			} finally { if ( tx.isActive() ) tx.rollback(); }
		}
		assertStored( scope, 1 );
	}

	@ParameterizedTest
	@ValueSource( strings = {"NONE", "READ_LOCK", "WRITE_LOCK", "DIRTY_FLUSH", "REPEAT_OPTIMISTIC", "REFRESH_OPTIMISTIC", "PFI_THEN_DIRTY_FLUSH", "CLEAR", "EVICT", "REPLACE_INSTANCE", "READ_ONLY", "ROLLBACK"} )
	void completionControls( String control, SessionFactoryScope scope ) {
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var tx = session.beginTransaction();
			try {
				final var item = Source.LOCK.schedule( session );
				switch ( control ) {
					case "READ_LOCK" -> session.lock( item, LockMode.PESSIMISTIC_READ );
					case "WRITE_LOCK" -> session.lock( item, LockMode.PESSIMISTIC_WRITE );
					case "DIRTY_FLUSH" -> { item.name = "changed"; session.flush(); }
					case "REPEAT_OPTIMISTIC" -> session.lock( item, OPTIMISTIC_FORCE_INCREMENT );
					case "REFRESH_OPTIMISTIC" -> session.refresh( item, OPTIMISTIC_FORCE_INCREMENT );
					case "PFI_THEN_DIRTY_FLUSH" -> { session.lock( item, PESSIMISTIC_FORCE_INCREMENT ); item.name = "changed"; session.flush(); }
					case "CLEAR" -> session.clear();
					case "EVICT" -> session.evict( item );
					case "REPLACE_INSTANCE" -> { session.evict( item ); session.find( Item.class, 1 ); }
					case "READ_ONLY" -> session.setReadOnly( item, true );
				}
				if ( control.equals( "ROLLBACK" ) ) tx.rollback(); else tx.commit();
				final int expected = switch ( control ) {
					case "CLEAR", "EVICT", "REPLACE_INSTANCE", "ROLLBACK" -> 0;
					case "DIRTY_FLUSH" -> 2;
					case "PFI_THEN_DIRTY_FLUSH" -> 3;
					default -> 1;
				};
				assertThat( item.version ).isEqualTo( expected );
				assertThat( updates( scope ) ).isEqualTo( expected );
			} finally { if ( tx.isActive() ) tx.rollback(); }
		}
	}

	@ParameterizedTest
	@org.hibernate.testing.orm.junit.RequiresDialectFeature( feature = org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsWait.class )
	@MethodSource( "failedPaths" )
	void failedScopeRetainsScheduledOptimisticIncrement( Path path, boolean commit, SessionFactoryScope scope ) {
		int expected = 0;
		try ( var owner = scope.getSessionFactory().openSession(); var blocker = scope.getSessionFactory().openSession() ) {
			final var tx = owner.beginTransaction();
			final var block = blocker.beginTransaction();
			try {
				final var item = Source.LOCK.schedule( owner );
				blocker.doWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						assertThat( statement.executeUpdate( "update scheduled_force_tags set tag_value='blocked' where owner_id=1" ) ).isEqualTo( 1 );
					}
				} );
				assertThatThrownBy( () -> {
					if ( path == Path.LOCK_EXTENDED ) owner.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED, Timeout.seconds( 1 ) );
					else owner.createQuery( "from ScheduledForceItem where id=1", Item.class )
							.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
							.setFollowOnStrategy( path == Path.DIRECT_QUERY_EXTENDED ? Locking.FollowOn.DISALLOW : Locking.FollowOn.FORCE )
							.setLockTimeout( Timeout.seconds( 1 ) ).getSingleResult();
				} )
						.isInstanceOfAny( jakarta.persistence.LockTimeoutException.class, jakarta.persistence.PessimisticLockException.class,
								org.hibernate.dialect.lock.internal.PessimisticEntityLockException.class,
								org.hibernate.exception.LockTimeoutException.class, org.hibernate.exception.LockAcquisitionException.class );
				assertThat( item.version ).isZero();
				// A statement-only timeout permits the original optimistic work to
				// complete; transaction-aborting dialects must instead roll back.
				final boolean canCommit = commit && !tx.getRollbackOnly();
				block.rollback();
				if ( canCommit ) tx.commit(); else tx.rollback();
				expected = canCommit ? 1 : 0;
				assertThat( item.version ).isEqualTo( expected );
				assertThat( updates( scope ) ).isEqualTo( expected );
			} finally {
				if ( tx.isActive() ) tx.rollback();
				if ( block.isActive() ) block.rollback();
			}
		}
		assertStored( scope, expected );
	}

	static Stream<Arguments> failedPaths() {
		return Stream.of( Path.LOCK_EXTENDED, Path.DIRECT_QUERY_EXTENDED, Path.FOLLOW_ON_QUERY_EXTENDED )
				.flatMap( path -> Stream.of( false, true ).map( commit -> Arguments.of( path, commit ) ) );
	}

	@ParameterizedTest
	@ValueSource( booleans = {false, true} )
	void laterTransactionDoesNotReuseOldProcess( boolean commitFirst, SessionFactoryScope scope ) {
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var first = session.beginTransaction();
			Source.LOCK.schedule( session );
			if ( commitFirst ) first.commit(); else first.rollback();
			final var second = session.beginTransaction();
			try {
				final var item = session.find( Item.class, 1 );
				session.lock( item, PESSIMISTIC_FORCE_INCREMENT );
				second.commit();
				assertThat( item.version ).isEqualTo( commitFirst ? 2 : 1 );
			} finally { if ( second.isActive() ) second.rollback(); }
		}
		assertStored( scope, commitFirst ? 2 : 1 );
	}

	enum Source {
		LOCK, FIND, QUERY;
		Item schedule( Session session ) {
			if ( this == FIND ) return session.find( Item.class, 1, OPTIMISTIC_FORCE_INCREMENT );
			if ( this == QUERY ) return session.createQuery( "from ScheduledForceItem where id=1", Item.class )
					.setHibernateLockMode( OPTIMISTIC_FORCE_INCREMENT ).getSingleResult();
			final var item = session.find( Item.class, 1 );
			session.lock( item, OPTIMISTIC_FORCE_INCREMENT );
			return item;
		}
	}

	enum Path {
		LOCK_NORMAL, LOCK_EXTENDED, FIND_EXTENDED, DIRECT_QUERY_EXTENDED, FOLLOW_ON_QUERY_EXTENDED;
		void acquire( Session session, Item item ) {
			switch ( this ) {
				case LOCK_NORMAL -> session.lock( item, PESSIMISTIC_FORCE_INCREMENT );
				case LOCK_EXTENDED -> session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED );
				case FIND_EXTENDED -> assertThat( session.find( Item.class, 1, PESSIMISTIC_FORCE_INCREMENT, EXTENDED ) ).isSameAs( item );
				default -> assertThat( session.createQuery( "from ScheduledForceItem where id=1", Item.class )
						.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
						.setFollowOnStrategy( this == DIRECT_QUERY_EXTENDED ? Locking.FollowOn.DISALLOW : Locking.FollowOn.FORCE )
						.getSingleResult() ).isSameAs( item );
			}
		}
	}

	static long updates( SessionFactoryScope scope ) {
		return scope.getCollectingStatementObserver().getSqlQueries().stream().filter( sql -> sql.startsWith( "update scheduled_force_item " ) ).count();
	}

	static void assertStored( SessionFactoryScope scope, int expected ) {
		scope.inTransaction( s -> assertThat( s.find( Item.class, 1 ).version ).isEqualTo( expected ) );
		scope.getSessionFactory().getCache().evictEntityData( Item.class );
		scope.inTransaction( s -> assertThat( s.find( Item.class, 1 ).version ).isEqualTo( expected ) );
	}

	@Entity( name = "ScheduledForceItem" )
	@Table( name = "scheduled_force_item" )
	@Cacheable
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	static class Item {
		@Id int id = 1;
		@Version int version;
		String name = "initial";
		@ElementCollection
		@CollectionTable( name = "scheduled_force_tags", joinColumns = @JoinColumn( name = "owner_id" ) )
		@Column( name = "tag_value" )
		Set<String> tags = new HashSet<>( Set.of( "tag" ) );
	}
}
