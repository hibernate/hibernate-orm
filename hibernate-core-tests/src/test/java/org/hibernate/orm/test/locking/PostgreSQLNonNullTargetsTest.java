package org.hibernate.orm.test.locking;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.internal.SelectionQueryImpl;
import org.hibernate.query.sqm.sql.spi.SqmTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static jakarta.persistence.PessimisticLockScope.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.LockMode.PESSIMISTIC_FORCE_INCREMENT;
import static org.hibernate.LockMode.PESSIMISTIC_READ;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/// Verifies direct locks on preserved PostgreSQL targets, the unchanged fallback
/// boundaries, and physical root-only protection at READ_COMMITTED.
///
/// @author Steve Ebersole
@RequiresDialect(PostgreSQLDialect.class)
@ExtendWith(FollowOnLockingTestSupport.ReadCommittedEligibility.class)
@DomainModel(overrideCacheStrategy = false, annotatedClasses = {
		PostgreSQLNonNullTargetsTest.Root.class, PostgreSQLNonNullTargetsTest.Detail.class,
		PostgreSQLNonNullTargetsTest.CompositeRoot.class, PostgreSQLNonNullTargetsTest.SecondaryRoot.class,
		PostgreSQLNonNullTargetsTest.JoinedRoot.class, PostgreSQLNonNullTargetsTest.JoinedSub.class,
		PostgreSQLNonNullTargetsTest.VersionedRoot.class
})
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@Tag("db-locking")
class PostgreSQLNonNullTargetsTest {
	static final String BASE = "from NonNullRoot r left join r.detail d where d.label is null or d.label <> 'excluded' order by r.id";

	@BeforeEach
	void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var second = new Detail();
			second.id = 2;
			second.label = "second";
			final var first = new Detail();
			first.id = 1;
			first.label = "first";
			first.next = second;
			session.persist( second );
			session.persist( first );
			for ( int id = 1; id <= 3; id++ ) {
				final var root = new Root();
				root.id = id;
				root.name = "root" + id;
				root.detail = id == 1 ? first : id == 3 ? second : null;
				root.tags.add( "tag" );
				session.persist( root );
			}
			for ( int local = 1; local <= 2; local++ ) {
				final var root = new CompositeRoot();
				root.id = new Key( 1, local );
				root.detail = local == 1 ? first : null;
				session.persist( root );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) { scope.dropData(); }

	static Stream<Arguments> directCases() {
		return Stream.of( PESSIMISTIC_READ, PESSIMISTIC_WRITE, PESSIMISTIC_FORCE_INCREMENT )
				.flatMap( mode -> Stream.of( Locking.FollowOn.ALLOW, Locking.FollowOn.DISALLOW, Locking.FollowOn.IGNORE )
						.flatMap( policy -> Stream.of( false, true ).flatMap( readOnly -> Stream.of( false, true )
								.map( fetched -> arguments( mode, policy, readOnly, fetched ) ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("directCases")
	void locksInTheOriginalQuery(LockMode mode, Locking.FollowOn policy, boolean readOnly, boolean fetched, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( readOnly );
			final var query = query( session, fetched ? BASE.replace( "join r.detail", "join fetch r.detail" ) : BASE, Root.class, mode, policy );
			assertDirect( session, query );
			scope.getCollectingStatementObserver().clear();
			final var roots = query.getResultList();
			assertThat( roots ).extracting( root -> root.id ).containsExactly( 1, 2, 3 );
			for ( var root : roots ) {
				assertThat( root.detail == null || Hibernate.isInitialized( root.detail ) ).isEqualTo( root.detail == null || fetched );
				if ( fetched && root.detail != null ) {
					assertThat( session.getCurrentLockMode( root.detail ) ).isEqualTo( LockMode.READ );
				}
				final var entry = session.getPersistenceContextInternal().getEntry( root );
				assertThat( entry.isReadOnly() ).isEqualTo( readOnly );
				assertThat( session.getCurrentLockMode( root ) ).isEqualTo( mode );
				assertThat( root.version ).isEqualTo( mode == PESSIMISTIC_FORCE_INCREMENT ? 1 : 0 );
				assertThat( entry.getVersion() ).isEqualTo( root.version );
				if ( readOnly ) {
					assertThat( entry.getLoadedState() ).isNull();
				}
				else {
					assertThat( entry.getLoadedState()[entry.getPersister().getVersionPropertyIndex()] ).isEqualTo( root.version );
				}
			}
			assertThat( sql( scope ).stream().filter( statement -> statement.startsWith( "select " ) ).count() ).isEqualTo( 1 );
			assertThat( sql( scope ).stream().filter( statement -> statement.startsWith( "update nn_root " ) ).count() )
					.isEqualTo( mode == PESSIMISTIC_FORCE_INCREMENT ? 3 : 0 );
		} );
	}

	@ParameterizedTest
	@EnumSource(value = LockMode.class, names = { "PESSIMISTIC_READ", "PESSIMISTIC_WRITE", "PESSIMISTIC_FORCE_INCREMENT" })
	void protectsRootsAndLeavesChildrenAvailable(LockMode mode, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( query( session, BASE, Root.class, mode, Locking.FollowOn.DISALLOW ).getResultList() ).hasSize( 3 );
			scope.inTransaction( contender -> {
				FollowOnLockingTestSupport.assertReadCommitted( contender );
				assertThat( contender.createNativeQuery( "select id from nn_root for update skip locked", Integer.class ).getResultList() ).isEmpty();
				assertThat( contender.createNativeQuery( "select id from nn_detail order by id for update nowait", Integer.class ).getResultList() )
						.containsExactly( 1, 2 );
			} );
		} );
	}

	@ParameterizedTest
	@CsvSource({ "PESSIMISTIC_READ,false", "PESSIMISTIC_READ,true", "PESSIMISTIC_WRITE,false", "PESSIMISTIC_WRITE,true",
			"PESSIMISTIC_FORCE_INCREMENT,false", "PESSIMISTIC_FORCE_INCREMENT,true" })
	void supportsCompositeIdentifiers(LockMode mode, boolean readOnly, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( readOnly );
			final var query = query( session, "from NonNullCompositeRoot r left join fetch r.detail d order by r.id.local", CompositeRoot.class, mode, Locking.FollowOn.DISALLOW );
			assertDirect( session, query );
			final var roots = query.getResultList();
			assertThat( roots ).hasSize( 2 );
			roots.forEach( root -> assertThat( root.version ).isEqualTo( mode == PESSIMISTIC_FORCE_INCREMENT ? 1 : 0 ) );
			scope.inTransaction( contender -> assertThat( contender.createNativeQuery( "select tenant from nn_composite for update skip locked", Integer.class ).getResultList() ).isEmpty() );
		} );
	}

	@Test
	void preservesAnExistingChildLock(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var detail = session.find( Detail.class, 1, PESSIMISTIC_WRITE, NORMAL );
			final var roots = query( session, BASE.replace( "join r.detail", "join fetch r.detail" ),
					Root.class, PESSIMISTIC_READ, Locking.FollowOn.DISALLOW ).getResultList();
			assertThat( roots.get( 0 ).detail ).isSameAs( detail );
			assertThat( session.getCurrentLockMode( detail ) ).isEqualTo( PESSIMISTIC_WRITE );
		} );
	}

	@ParameterizedTest
	@CsvSource({ "PESSIMISTIC_READ,false", "PESSIMISTIC_READ,true", "PESSIMISTIC_WRITE,false", "PESSIMISTIC_WRITE,true",
			"PESSIMISTIC_FORCE_INCREMENT,false", "PESSIMISTIC_FORCE_INCREMENT,true" })
	void preservesFetchedChildVersions(LockMode mode, boolean readOnly, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var root = new VersionedRoot();
			root.id = 1;
			root.child = session.getReference( Root.class, 1 );
			session.persist( root );
		} );
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( readOnly );
			final var query = query( session, "from NonNullVersionedRoot r left join fetch r.child", VersionedRoot.class,
					mode, Locking.FollowOn.DISALLOW );
			assertDirect( session, query );
			final var root = query.getSingleResult();
			assertThat( root.version ).isEqualTo( mode == PESSIMISTIC_FORCE_INCREMENT ? 1 : 0 );
			assertThat( root.child.version ).isZero();
			assertThat( session.getCurrentLockMode( root.child ) ).isEqualTo( LockMode.READ );
			final var childEntry = session.getPersistenceContextInternal().getEntry( root.child );
			assertThat( childEntry.getVersion() ).isEqualTo( 0 );
			if ( !readOnly ) {
				assertThat( childEntry.getLoadedValue( "version" ) ).isEqualTo( 0 );
			}
		} );
	}

	@Test
	void preservesManagedDirtyState(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var root = session.find( Root.class, 1 );
			root.name = "local change";
			session.setHibernateFlushMode( FlushMode.MANUAL );
			final var query = query( session, BASE, Root.class, PESSIMISTIC_WRITE, Locking.FollowOn.DISALLOW );
			assertDirect( session, query );
			scope.getCollectingStatementObserver().clear();
			assertThat( query.getResultList().get( 0 ) ).isSameAs( root );
			assertThat( root.name ).isEqualTo( "local change" );
			final var entry = session.getPersistenceContextInternal().getEntry( root );
			assertThat( entry.getLoadedValue( "name" ) ).isEqualTo( "root1" );
			assertThat( sql( scope ) ).hasSize( 1 );
		} );
	}

	@Test
	void retainsForcedFollowOnAcquisition(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = query( session, BASE, Root.class, PESSIMISTIC_WRITE, Locking.FollowOn.FORCE );
			assertThat( plan( session, query ).getLockingApplication() ).isEqualTo( JdbcLockingApplication.FOLLOW_ON );
			scope.getCollectingStatementObserver().clear();
			assertThat( query.getResultList() ).hasSize( 3 );
			assertThat( sql( scope ).stream().filter( statement -> statement.startsWith( "select " ) && statement.contains( "from nn_root " ) ).count() ).isEqualTo( 2 );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"select r,d from NonNullRoot r left join r.detail d",
			"select d from NonNullRoot r left join r.detail d",
			"from NonNullRoot r right join r.detail d",
			"from NonNullRoot r full join r.detail d",
			"from NonNullRoot r left join r.tags t",
			"from NonNullRoot r left join r.detail d right join d.next n",
			"select distinct r from NonNullRoot r left join r.detail d",
			"select r from NonNullRoot r left join r.detail d group by r",
			"select r from NonNullRoot r left join r.detail d group by r having count(d)>0",
			"from NonNullSecondary r left join r.detail d",
			"from NonNullJoined r left join r.detail d"
	})
	void retainsFallbackForIneligibleShapes(String hql, SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var query = query( session, hql, Object.class, PESSIMISTIC_WRITE, Locking.FollowOn.ALLOW );
			assertThat( plan( session, query ).getLockingApplication() ).isEqualTo( JdbcLockingApplication.FOLLOW_ON );
			query.setFollowOnStrategy( Locking.FollowOn.DISALLOW );
			assertThatThrownBy( () -> plan( session, query ) ).isInstanceOf( IllegalQueryOperationException.class );
		} );
	}

	@ParameterizedTest
	@EnumSource(value = PessimisticLockScope.class, names = { "EXTENDED", "FETCHED" })
	void retainsWiderScopeFallback(PessimisticLockScope lockScope, SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var query = query( session, BASE, Root.class, PESSIMISTIC_WRITE, Locking.FollowOn.ALLOW ).setLockScope( lockScope );
			assertThat( plan( session, query ).getLockingApplication() ).isEqualTo( JdbcLockingApplication.FOLLOW_ON );
		} );
	}

	@ParameterizedTest
	@CsvSource({ "0,1", "1,0" })
	void retainsPaginationFallback(int first, int max, SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var query = query( session, BASE, Root.class, PESSIMISTIC_WRITE, Locking.FollowOn.ALLOW );
			if ( first > 0 ) { query.setFirstResult( first ); }
			if ( max > 0 ) { query.setMaxResults( max ); }
			assertThat( plan( session, query ).getLockingApplication() ).isEqualTo( JdbcLockingApplication.FOLLOW_ON );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "select r.name from NonNullRoot r left join r.detail d where d.label is null", "select count(r) from NonNullRoot r left join r.detail d" })
	void retainsScalarCoverageRejection(String hql, SessionFactoryScope scope) {
		scope.inSession( session -> assertThatThrownBy( () -> plan( session,
				query( session, hql, Object.class, PESSIMISTIC_WRITE, Locking.FollowOn.ALLOW ) ) ).isInstanceOf( IllegalQueryOperationException.class ) );
	}

	@Test
	void acceptsNestedOptionalToOneFetches(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = query( session, "from NonNullRoot r left join fetch r.detail d left join fetch d.next order by r.id", Root.class,
					PESSIMISTIC_WRITE, Locking.FollowOn.DISALLOW );
			assertDirect( session, query );
			assertThat( query.getResultList() ).hasSize( 3 );
		} );
	}

	@Test
	void nativeSkipLockedExcludesContendedRoots(SessionFactoryScope scope) {
		try ( var blocker = scope.getSessionFactory().openSession() ) {
			final var transaction = blocker.beginTransaction();
			try {
				blocker.find( Root.class, 1, PESSIMISTIC_WRITE );
				scope.inTransaction( session -> {
					final var query = query( session, BASE, Root.class, PESSIMISTIC_WRITE, Locking.FollowOn.DISALLOW ).setLockTimeout( Timeouts.SKIP_LOCKED );
					assertDirect( session, query );
					assertThat( query.getResultList() ).extracting( root -> root.id ).containsExactly( 2, 3 );
				} );
			}
			finally { transaction.rollback(); }
		}
	}

	@ParameterizedTest
	@CsvSource({ "true", "false" })
	void preservesNoWaitAndFiniteTimeoutFailures(boolean noWait, SessionFactoryScope scope) {
		try ( var blocker = scope.getSessionFactory().openSession() ) {
			final var transaction = blocker.beginTransaction();
			try {
				blocker.find( Root.class, 1, PESSIMISTIC_WRITE );
				try ( var session = scope.getSessionFactory().openSession() ) {
					final var readerTransaction = session.beginTransaction();
					try {
						final var query = query( session, BASE, Root.class, PESSIMISTIC_WRITE, Locking.FollowOn.DISALLOW )
								.setLockTimeout( noWait ? Timeouts.NO_WAIT : jakarta.persistence.Timeout.seconds( 1 ) );
						assertDirect( session, query );
						assertThatThrownBy( query::getResultList ).isExactlyInstanceOf( jakarta.persistence.PessimisticLockException.class )
								.hasCauseInstanceOf( org.hibernate.exception.LockTimeoutException.class );
						assertThat( session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isFalse();
					}
					finally { readerTransaction.rollback(); }
				}
			}
			finally { transaction.rollback(); }
		}
	}

	static <T> SelectionQuery<T> query(SessionImplementor session, String hql, Class<T> type, LockMode mode, Locking.FollowOn policy) {
		return session.createSelectionQuery( hql, type ).setHibernateLockMode( mode ).setLockScope( NORMAL ).setFollowOnStrategy( policy );
	}

	private static void assertDirect(SessionImplementor session, SelectionQuery<?> query) {
		final var operation = plan( session, query );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
		assertThat( operation.getSqlString() ).contains( " for " );
		final var clause = operation.getSqlString().substring( operation.getSqlString().lastIndexOf( " for " ) );
		final var rootAlias = Pattern.compile( " from nn_(?:root|composite|versioned_root) ([^ ]+)" ).matcher( operation.getSqlString() );
		assertThat( rootAlias.find() ).isTrue();
		assertThat( clause ).matches( " for (update|no key update|share) of " + Pattern.quote( rootAlias.group( 1 ) )
				+ "(?: nowait| skip locked)?" );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
	}

	static JdbcSelect plan(SessionImplementor session, SelectionQuery<?> selection) {
		final var query = (SelectionQueryImpl<?>) selection;
		try {
			final var interpretation = session.getFactory().getQueryEngine().getSqmTranslatorFactory().createSelectTranslator(
					new SqmTranslationRequest.Select( query.getSqmStatement(), query.getQueryOptions(), query.getDomainParameterXref(),
							query.getParameterBindings(), session.getLoadQueryInfluencers(), session.getFactory().getSqlTranslationEngine(), true ) ).translate();
			return session.getDialect().getSqlAstTranslatorFactory().buildTranslator( new SqlAstTranslationRequest.Select( session.getFactory(), interpretation.getSqlAst() ) )
					.translate( JdbcParameterBindings.NO_BINDINGS, query.getQueryOptions() );
		}
		finally { query.getDomainParameterXref().clearExpansions(); }
	}

	private static List<String> sql(SessionFactoryScope scope) { return scope.getCollectingStatementObserver().getSqlQueries(); }

	@Entity(name = "NonNullRoot") @Table(name = "nn_root")
	static class Root {
		@Id int id;
		String name;
		@Version int version;
		@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "detail_id") Detail detail;
		@ElementCollection Set<String> tags = new HashSet<>();
	}

	@Entity(name = "NonNullDetail") @Table(name = "nn_detail")
	static class Detail {
		@Id int id;
		String label;
		@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "next_id") Detail next;
	}

	@Embeddable
	static class Key implements Serializable {
		int tenant;
		int local;
		Key() {}
		Key(int tenant, int local) { this.tenant = tenant; this.local = local; }
		@Override public boolean equals(Object other) { return other instanceof Key key && tenant == key.tenant && local == key.local; }
		@Override public int hashCode() { return Objects.hash( tenant, local ); }
	}

	@Entity(name = "NonNullCompositeRoot") @Table(name = "nn_composite")
	static class CompositeRoot {
		@EmbeddedId Key id;
		@Version int version;
		@ManyToOne(fetch = FetchType.LAZY) Detail detail;
	}

	@Entity(name = "NonNullVersionedRoot") @Table(name = "nn_versioned_root")
	static class VersionedRoot {
		@Id int id;
		@Version int version;
		@ManyToOne(fetch = FetchType.LAZY) Root child;
	}

	@Entity(name = "NonNullSecondary") @Table(name = "nn_secondary_root") @SecondaryTable(name = "nn_secondary_detail")
	static class SecondaryRoot {
		@Id int id;
		@Column(table = "nn_secondary_detail") String note;
		@ManyToOne(fetch = FetchType.LAZY) Detail detail;
	}

	@Entity(name = "NonNullJoined") @Table(name = "nn_joined") @Inheritance(strategy = InheritanceType.JOINED)
	static class JoinedRoot {
		@Id int id;
		@ManyToOne(fetch = FetchType.LAZY) Detail detail;
	}

	@Entity(name = "NonNullJoinedSub") @Table(name = "nn_joined_sub")
	static class JoinedSub extends JoinedRoot { String note; }
}
