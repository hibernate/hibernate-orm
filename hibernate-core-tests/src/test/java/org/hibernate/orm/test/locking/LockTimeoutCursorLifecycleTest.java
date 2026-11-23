package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;

import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.lock.internal.H2LockingSupport;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolver;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Cursor handoff and disposal with controlled connection-timeout routing.
/// H2's production timeout capabilities are intentionally unchanged.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = LockTimeoutCursorLifecycleTest.Item.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.dialect", value = "org.hibernate.orm.test.locking.LockTimeoutCursorLifecycleTest$TimeoutDialect"),
		@Setting(name = "hibernate.connection.provider_class", value = "org.hibernate.orm.test.locking.LockTimeoutConnectionProvider"),
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@Jira("https://hibernate.atlassian.net/browse/HHH-21033")
class LockTimeoutCursorLifecycleTest {
	private static final int BASELINE = 1000;
	private static final int REQUEST = 500;
	private static final ThreadLocal<Observation> CURRENT = new ThreadLocal<>();

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1 ) );
			session.persist( new Item( 2 ) );
		} );
		provider( scope ).resetObservations();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		CURRENT.remove();
		provider( scope ).resetObservations();
		scope.dropData();
	}

	@Test
	void failedRestorationBeforeHandoffReleasesUnreturnedCursor(SessionFactoryScope scope) {
		withExecution( scope, (session, observation) -> {
			observation.restoreFailure = new IllegalStateException( "restore before handoff" );
			assertThatThrownBy( () -> query( session, false ).scroll( ScrollMode.FORWARD_ONLY ) )
					.isSameAs( observation.restoreFailure );
			assertThat( observation.timeouts ).containsExactly( REQUEST, BASELINE );
			assertResourcesClosed( scope, session );
			// A failed setter is observable. Resource disposal does not claim to repair it.
			assertThat( session.doReturningWork( LockTimeoutCursorLifecycleTest::read ) ).isEqualTo( REQUEST );
		} );
	}

	@Test
	void setupFailureAfterSqlExecutionRestoresAndReleases(SessionFactoryScope scope) {
		withExecution( scope, (session, observation) -> {
			final var original = new IllegalStateException( "setup after executeQuery" );
			session.addEventListeners( new org.hibernate.SessionEventListener() {
				@Override public void jdbcExecuteStatementEnd() { throw original; }
			} );
			assertThatThrownBy( () -> query( session, false ).scroll( ScrollMode.FORWARD_ONLY ) ).isSameAs( original );
			assertThat( observation.timeouts ).containsExactly( REQUEST, BASELINE );
			assertResourcesClosed( scope, session );
		} );
	}

	@Test
	void alreadyReleasedConsumerIsNotClosedAgain(SessionFactoryScope scope) {
		withExecution( scope, (session, observation) -> {
			final var original = new IllegalStateException( "tuple transformation" );
			assertThatThrownBy( () -> query( session, false ).setTupleTransformer( (tuple, aliases) -> {
				throw original;
			} ).getResultList() ).isSameAs( original );
			assertThat( observation.timeouts ).containsExactly( REQUEST, BASELINE );
			assertResourcesClosed( scope, session );
		} );
	}

	@Test
	void failedHandoffLeavesOtherCursorUsable(SessionFactoryScope scope) {
		withExecution( scope, (session, observation) -> {
			try ( var first = query( session, false ).scroll( ScrollMode.FORWARD_ONLY ) ) {
				observation.restoreFailure = new IllegalStateException( "second cursor restoration" );
				assertThatThrownBy( () -> query( session, false ).scroll( ScrollMode.FORWARD_ONLY ) )
						.isSameAs( observation.restoreFailure );
				assertThat( provider( scope ).resources ).hasSize( 2 );
				assertThat( provider( scope ).resources.get( 0 ).resultSetCloses ).isZero();
				assertThat( provider( scope ).resources.get( 1 ).resultSetCloses ).isEqualTo( 1 );
				assertThat( first.next() ).isTrue();
				assertThat( first.get().id ).isEqualTo( 1 );
			}
			assertResourcesClosed( scope, session );
		} );
	}

	static Stream<Arguments> cursorCases() {
		return Stream.of( false, true ).flatMap( stream -> Stream.of( false, true ).flatMap( followOn ->
				Stream.of( "empty", "early", "full", "failure" ).map( completion -> Arguments.of( stream, followOn, completion ) ) ) );
	}

	@ParameterizedTest(name = "stream={0}, followOn={1}, completion={2}")
	@MethodSource("cursorCases")
	void cursorLifecycle(boolean stream, boolean followOn, String completion, SessionFactoryScope scope) {
		withExecution( scope, (session, observation) -> {
			final var query = query( session, followOn );
			final var original = new IllegalStateException( "fetch failure" );
			try ( var cursor = stream ? Cursor.stream( query ) : Cursor.scroll( query ) ) {
				assertThat( session.doReturningWork( LockTimeoutCursorLifecycleTest::read ) ).isEqualTo( BASELINE );
				assertThat( observation.timeouts ).containsExactly( REQUEST, BASELINE );
				assertThat( provider( scope ).resources.get( 0 ).resultSetCloses ).isZero();
				if ( completion.equals( "failure" ) ) {
					provider( scope ).nextFailure = original;
					assertThatThrownBy( cursor::next ).isSameAs( original );
				}
				else if ( !completion.equals( "empty" ) ) {
					assertThat( cursor.next() ).isTrue();
					if ( completion.equals( "full" ) ) {
						assertThat( cursor.next() ).isTrue();
						assertThat( cursor.next() ).isFalse();
					}
				}
				assertThat( session.doReturningWork( LockTimeoutCursorLifecycleTest::read ) ).isEqualTo( BASELINE );
			}
			final int acquiredRows = completion.equals( "full" ) ? 2 : completion.equals( "early" ) ? 1 : 0;
			final int executions = 1 + (followOn ? acquiredRows : 0);
			assertThat( observation.timeouts ).hasSize( executions * 2 );
			for ( int i = 0; i < executions; i++ ) {
				assertThat( observation.timeouts.subList( i * 2, i * 2 + 2 ) ).containsExactly( REQUEST, BASELINE );
			}
			assertThat( original.getSuppressed() ).isEmpty();
			assertResourcesClosed( scope, session );
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void consumptionFailureKeepsCloseFailureSuppressed(boolean stream, SessionFactoryScope scope) {
		withExecution( scope, (session, observation) -> {
			final var primary = new IllegalStateException( "fetch" );
			final var close = new AssertionError( "physical statement close" );
			assertThatThrownBy( () -> {
				try ( var cursor = stream ? Cursor.stream( query( session, false ) ) : Cursor.scroll( query( session, false ) ) ) {
					provider( scope ).nextFailure = primary;
					provider( scope ).closeFailure = close;
					cursor.next();
				}
			} ).isSameAs( primary );
			assertThat( primary.getSuppressed() ).containsExactly( close );
			assertThat( observation.timeouts ).containsExactly( REQUEST, BASELINE );
			assertResourcesClosed( scope, session );
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void failedOperationRollsBackAndReusesPhysicalConnection(boolean aggressive, SessionFactoryScope scope) {
		final var provider = provider( scope );
		final var mode = aggressive ? PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT
				: PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION;
		final var observation = new Observation();
		CURRENT.set( observation );
		Connection firstConnection;
		try {
			try ( var session = scope.getSessionFactory().withOptions().connectionHandlingMode( mode ).openSession() ) {
				final var tx = session.beginTransaction();
				firstConnection = session.doReturningWork( c -> { set( c, BASELINE ); return c; } );
				provider.events.clear();
				provider.onReturn = c -> {
					assertThat( readUnchecked( c ) ).as( "timeout at connection return" ).isEqualTo( BASELINE );
				};
				final var original = new IllegalStateException( "consumer" );
				assertThatThrownBy( () -> query( session, false ).setTupleTransformer( (tuple, aliases) -> { throw original; } )
						.getResultList() ).isSameAs( original );
				assertThat( observation.timeouts ).containsExactly( REQUEST, BASELINE );
				assertThat( session.unwrap( SessionImplementor.class ).getJdbcCoordinator().getLogicalConnection()
						.getConnectionHandlingMode() ).isEqualTo( mode );
				if ( aggressive ) {
					assertThat( provider.events ).as( "aggressive release before rollback" ).contains( "return" );
				}
				tx.rollback();
			}
			try ( var session = scope.getSessionFactory().openSession() ) {
				final var tx = session.beginTransaction();
				session.doWork( c -> {
					assertThat( c ).isSameAs( firstConnection );
					assertThat( read( c ) ).isEqualTo( BASELINE );
				} );
				assertThat( session.createQuery( "select count(*) from TimeoutLifecycleItem", Long.class ).getSingleResult() ).isEqualTo( 2 );
				tx.rollback();
			}
			assertThat( provider.events ).contains( "return", "borrow" );
			assertThat( provider.borrowedConnections ).isNotEmpty().allMatch( c -> c == provider.returnedConnections.get( 0 ) );
			assertThat( provider.returnedConnections ).isNotEmpty().allMatch( c -> c == provider.borrowedConnections.get( 0 ) );
		}
		finally {
			provider.onReturn = c -> {};
			CURRENT.remove();
		}
	}

	private static Query<Item> query(Session session, boolean followOn) {
		return session.createQuery( "from TimeoutLifecycleItem order by id", Item.class )
				.setLockMode( PESSIMISTIC_WRITE ).setLockTimeout( Timeout.milliseconds( REQUEST ) )
				.setFollowOnStrategy( followOn ? Locking.FollowOn.FORCE : Locking.FollowOn.DISALLOW );
	}

	private static void withExecution(SessionFactoryScope scope, Work work) {
		final var observation = new Observation();
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var tx = session.beginTransaction();
			session.doWork( c -> set( c, BASELINE ) );
			CURRENT.set( observation );
			provider( scope ).onExecute = c -> assertThat( readUnchecked( c ) ).isEqualTo( REQUEST );
			try {
				work.run( session, observation );
			}
			finally {
				CURRENT.remove();
				provider( scope ).resetObservations();
				session.doWork( c -> set( c, BASELINE ) );
				tx.rollback();
			}
		}
	}

	private static void assertResourcesClosed(SessionFactoryScope scope, Session session) {
		assertThat( provider( scope ).resources ).isNotEmpty().allSatisfy( resource -> {
			assertThat( resource.resultSetCloses ).isEqualTo( 1 );
			assertThat( resource.statementCloses ).isEqualTo( 1 );
		} );
		assertThat( session.unwrap( SessionImplementor.class ).getJdbcCoordinator().getLogicalConnection()
				.getResourceRegistry().hasRegisteredResources() ).isFalse();
	}

	private static LockTimeoutConnectionProvider provider(SessionFactoryScope scope) {
		return (LockTimeoutConnectionProvider) scope.getSessionFactory().getServiceRegistry().getService( ConnectionProvider.class );
	}

	private static int read(Connection c) throws SQLException {
		try ( var statement = c.createStatement(); var rows = statement.executeQuery( "call lock_timeout()" ) ) {
			rows.next();
			return rows.getInt( 1 );
		}
	}

	private static int readUnchecked(Connection c) {
		try { return read( c ); }
		catch (SQLException e) { throw new IllegalStateException( e ); }
	}

	private static void set(Connection c, int timeout) throws SQLException {
		try ( var statement = c.createStatement() ) { statement.executeUpdate( "set lock_timeout " + timeout ); }
	}

	private interface Work { void run(Session session, Observation observation); }

	private static class Observation {
		final List<Integer> timeouts = new ArrayList<>();
		RuntimeException restoreFailure;
	}

	private record Cursor(java.util.function.BooleanSupplier advance, AutoCloseable resource) implements AutoCloseable {
		boolean next() { return advance.getAsBoolean(); }
		static Cursor stream(Query<Item> query) {
			final var stream = query.getResultStream();
			final var iterator = stream.iterator();
			return new Cursor( () -> { if ( !iterator.hasNext() ) { return false; } iterator.next(); return true; }, stream );
		}
		static Cursor scroll(Query<Item> query) {
			final var scroll = query.scroll( ScrollMode.FORWARD_ONLY );
			return new Cursor( scroll::next, scroll );
		}
		@Override public void close() {
			try { resource.close(); }
			catch (RuntimeException | Error e) { throw e; }
			catch (Exception e) { throw new IllegalStateException( e ); }
		}
	}

	/// Routes H2 requests through real connection commands for lifecycle tests.
	public static class TimeoutDialect extends H2Dialect {
		@Override public LockingSupport getLockingSupport() { return new TimeoutSupport(); }
	}

	private static class TimeoutSupport implements LockingSupport, LockingSupport.Metadata, ConnectionLockTimeoutStrategy {
		@Override public Metadata getMetadata() { return this; }
		@Override public OuterJoinLockingType getOuterJoinLockingType() { return OuterJoinLockingType.IGNORED; }
		@Override public LockTimeoutType getLockTimeoutType(Timeout timeout) { return LockTimeoutType.CONNECTION; }
		@Override public LockingClauseRenderer getLockingClauseRenderer() { return request -> " for update"; }
		@Override public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() { return this; }
		@Override public TransactionConcurrencyResolver getTransactionConcurrencyResolver() { return H2LockingSupport.INSTANCE.getTransactionConcurrencyResolver(); }
		@Override public Level getSupportedLevel() { return Level.SUPPORTED; }
		@Override public Timeout getLockTimeout(Connection c, SessionFactoryImplementor factory) { return Timeout.milliseconds( readUnchecked( c ) ); }
		@Override public void setLockTimeout(Timeout timeout, Connection c, SessionFactoryImplementor factory) {
			final var observation = CURRENT.get();
			if ( observation != null ) {
				observation.timeouts.add( timeout.milliseconds() );
				if ( timeout.milliseconds() == BASELINE && observation.restoreFailure != null ) { throw observation.restoreFailure; }
			}
			try { set( c, timeout.milliseconds() ); }
			catch (SQLException e) { throw new IllegalStateException( e ); }
		}
	}

	@Entity(name = "TimeoutLifecycleItem")
	@Table(name = "timeout_lifecycle_item")
	static class Item {
		@Id int id;
		Item() {}
		Item(int id) { this.id = id; }
	}
}
