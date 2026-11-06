package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;

import org.hibernate.JDBCException;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/// Real acquisition errors, transaction-local versus persistent settings, and
/// physical connection reuse through production CONNECTION timeout routing.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = ProductionLockTimeoutLifecycleTest.Item.class)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.provider_class", value = "org.hibernate.orm.test.locking.LockTimeoutConnectionProvider"),
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@ExtendWith(ProductionLockTimeoutLifecycleTest.Eligibility.class)
@Tag("db-locking")
@Jira("https://hibernate.atlassian.net/browse/HHH-21033")
public class ProductionLockTimeoutLifecycleTest {
	private static final Timeout SESSION_BASELINE = Timeout.seconds( 4 );
	private static final Timeout TRANSACTION_BASELINE = Timeout.seconds( 2 );
	private static final Timeout REQUEST = Timeout.seconds( 1 );

	/// Requires the actual production timeout route, retaining the Sybase exclusion.
	public static class Eligibility extends ProductionConnectionLockTimeoutTest.Eligibility {
		@Override
		public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
			final var dialect = org.hibernate.testing.orm.junit.DialectContext.getDialect();
			return dialect instanceof PostgreSQLDialect || dialect instanceof SQLServerDialect
					? super.evaluateExecutionCondition( context )
					: ConditionEvaluationResult.disabled( "Requires PostgreSQL or SQL Server transaction/error semantics" );
		}
	}

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			session.persist( new Item( 1 ) );
			session.persist( new Item( 2 ) );
		} );
		provider( scope ).resetObservations();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		provider( scope ).resetObservations();
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void acquisitionFailureRollsBackAndReusesPhysicalConnection(boolean followOn, SessionFactoryScope scope) throws SQLException {
		final var factory = scope.getSessionFactory();
		final var provider = provider( scope );
		final var strategy = factory.getJdbcServices().getDialect().getLockingSupport().getConnectionLockTimeoutStrategy();
		final boolean postgres = factory.getJdbcServices().getDialect() instanceof PostgreSQLDialect;
		// This is a second physical connection, outside the one-connection fixture.
		// Acquiring the update synchronously establishes contention before the reader runs.
		final var blocker = provider.getConnectionProvider().getConnection();
		try {
			blocker.setTransactionIsolation( Connection.TRANSACTION_READ_COMMITTED );
			blocker.setAutoCommit( false );
			try ( var statement = blocker.createStatement() ) {
				statement.setQueryTimeout( 10 );
				assertThat( statement.executeUpdate( "update timeout_lifecycle_item set marker = marker + 1 where id = 1" ) ).isEqualTo( 1 );
			}
			try ( var baseline = new SessionBaseline( scope ) ) {
				final var firstConnection = baseline.connection;
				try ( var session = factory.openSession() ) {
					final var tx = session.beginTransaction();
					try {
						FollowOnLockingTestSupport.assertReadCommitted( session );
						strategy.setLockTimeout( TRANSACTION_BASELINE, firstConnection, factory );
						provider.events.clear();
						provider.onExecute = c -> assertThat( strategy.getLockTimeout( c, factory ).milliseconds() )
								.isEqualTo( REQUEST.milliseconds() );
						final var failure = catchThrowable( () -> session.createQuery( "from ProductionLifecycleItem where id = 1", Item.class )
								.setLockMode( PESSIMISTIC_WRITE ).setLockTimeout( REQUEST )
								.setFollowOnStrategy( followOn ? Locking.FollowOn.FORCE : Locking.FollowOn.DISALLOW )
								.setTimeout( Timeout.seconds( 10 ) ).getSingleResult() );
						assertThat( failure ).isNotNull();
						final var graph = exceptionGraph( failure );
						if ( postgres ) {
							assertThat( graph ).anySatisfy( throwable -> assertThat( sqlState( throwable ) ).isEqualTo( "55P03" ) );
							assertThat( graph ).anySatisfy( throwable -> {
								assertThat( throwable.getSuppressed() ).anySatisfy( suppressed ->
										assertThat( exceptionGraph( suppressed ) ).anySatisfy( cause ->
												assertThat( sqlState( cause ) ).isEqualTo( "25P02" ) ) );
							} );
						}
						else {
							assertThat( graph ).anySatisfy( throwable -> assertThat( errorCode( throwable ) ).isEqualTo( 1222 ) );
							assertThat( strategy.getLockTimeout( firstConnection, factory ).milliseconds() )
									.isEqualTo( TRANSACTION_BASELINE.milliseconds() );
							assertThat( graph ).allSatisfy( throwable -> assertThat( throwable.getSuppressed() ).isEmpty() );
							try ( var statement = firstConnection.createStatement(); var rows = statement.executeQuery( "select xact_state()" ) ) {
								assertThat( rows.next() ).isTrue();
								assertThat( rows.getInt( 1 ) ).as( "SQL Server statement timeout leaves a committable transaction" ).isEqualTo( 1 );
							}
						}
						assertThat( session.unwrap( SessionImplementor.class ).getJdbcCoordinator().getLogicalConnection()
								.getResourceRegistry().hasRegisteredResources() ).isFalse();
					}
					finally {
						provider.onExecute = c -> {};
						tx.rollback();
					}
				}
				assertReborrow( scope, firstConnection, postgres ? SESSION_BASELINE : TRANSACTION_BASELINE, strategy );
			}
		}
		finally {
			blocker.rollback();
			provider.getConnectionProvider().closeConnection( blocker );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void earlyCursorCloseRestoresBeforePhysicalReuse(boolean stream, SessionFactoryScope scope) throws SQLException {
		final var factory = scope.getSessionFactory();
		final var strategy = factory.getJdbcServices().getDialect().getLockingSupport().getConnectionLockTimeoutStrategy();
		final var provider = provider( scope );
		Connection connection;
		try ( var baseline = new SessionBaseline( scope ) ) {
			try ( var session = factory.openSession() ) {
				final var tx = session.beginTransaction();
				try {
					connection = session.doReturningWork( c -> c );
					strategy.setLockTimeout( TRANSACTION_BASELINE, connection, factory );
					provider.onExecute = c -> assertThat( strategy.getLockTimeout( c, factory ).milliseconds() ).isEqualTo( REQUEST.milliseconds() );
					final var query = session.createQuery( "from ProductionLifecycleItem order by id", Item.class )
							.setLockMode( PESSIMISTIC_WRITE ).setLockTimeout( REQUEST ).setFetchSize( 1 )
							.setFollowOnStrategy( Locking.FollowOn.DISALLOW );
					if ( stream ) {
						try ( var rows = query.getResultStream() ) {
							assertThat( strategy.getLockTimeout( connection, factory ).milliseconds() ).isEqualTo( TRANSACTION_BASELINE.milliseconds() );
							assertThat( rows.findFirst() ).isPresent();
						}
					}
					else {
						try ( var rows = query.scroll( ScrollMode.FORWARD_ONLY ) ) {
							assertThat( strategy.getLockTimeout( connection, factory ).milliseconds() ).isEqualTo( TRANSACTION_BASELINE.milliseconds() );
							assertThat( rows.next() ).isTrue();
						}
					}
					assertThat( provider.resources ).isNotEmpty().allSatisfy( resource -> {
						assertThat( resource.resultSetCloses ).isEqualTo( 1 );
						assertThat( resource.statementCloses ).isEqualTo( 1 );
					} );
				}
				finally { provider.onExecute = c -> {}; tx.rollback(); }
			}
			assertReborrow( scope, connection, baseline.postgres ? SESSION_BASELINE : TRANSACTION_BASELINE, strategy );
		}
	}

	private static void assertReborrow(SessionFactoryScope scope, Connection firstConnection, Timeout expected,
			ConnectionLockTimeoutStrategy strategy) {
		final var factory = scope.getSessionFactory();
		final var provider = provider( scope );
		try {
			provider.onBorrow = c -> {
				assertThat( c ).as( "same physical connection" ).isSameAs( firstConnection );
				assertThat( strategy.getLockTimeout( c, factory ).milliseconds() ).as( "timeout before fixture repair" )
						.isEqualTo( expected.milliseconds() );
			};
			try ( var session = factory.openSession() ) {
				final var tx = session.beginTransaction();
				try {
					assertThat( session.createQuery( "select count(*) from ProductionLifecycleItem", Long.class ).getSingleResult() ).isEqualTo( 2 );
				}
				finally { tx.rollback(); }
			}
			assertThat( provider.events ).contains( "return", "borrow" );
			assertThat( provider.borrowedConnections ).isNotEmpty().allMatch( c -> c == provider.returnedConnections.get( 0 ) );
			assertThat( provider.returnedConnections ).isNotEmpty().allMatch( c -> c == provider.borrowedConnections.get( 0 ) );
		}
		finally { provider.onBorrow = c -> {}; }
	}

	/// Repairs the fixture only after all return/reborrow assertions have run.
	private static class SessionBaseline implements AutoCloseable {
		final Connection connection;
		final boolean postgres;
		final Timeout original;
		final boolean autoCommit;

		SessionBaseline(SessionFactoryScope scope) throws SQLException {
			final var factory = scope.getSessionFactory();
			connection = provider( scope ).getConnection();
			postgres = factory.getJdbcServices().getDialect() instanceof PostgreSQLDialect;
			original = factory.getJdbcServices().getDialect().getLockingSupport()
					.getConnectionLockTimeoutStrategy().getLockTimeout( connection, factory );
			autoCommit = connection.getAutoCommit();
			// Establish S before a Session caches JDBC autocommit state.
			connection.setAutoCommit( true );
			try { setSessionBaseline( connection, postgres, SESSION_BASELINE ); }
			finally { connection.setAutoCommit( autoCommit ); }
		}

		@Override public void close() throws SQLException {
			connection.setAutoCommit( true );
			try { setSessionBaseline( connection, postgres, original ); }
			finally { connection.setAutoCommit( autoCommit ); }
		}
	}

	private static void setSessionBaseline(Connection connection, boolean postgres, Timeout timeout) throws SQLException {
		try ( var statement = connection.createStatement() ) {
			final int milliseconds = postgres && timeout.milliseconds() == org.hibernate.Timeouts.WAIT_FOREVER_MILLI
					? 0 : timeout.milliseconds();
			statement.executeUpdate( (postgres ? "set lock_timeout = " : "set lock_timeout ") + milliseconds );
		}
	}

	private static String sqlState(Throwable failure) {
		return failure instanceof JDBCException jdbc ? jdbc.getSQLState() : failure instanceof SQLException sql ? sql.getSQLState() : null;
	}

	private static int errorCode(Throwable failure) {
		return failure instanceof JDBCException jdbc ? jdbc.getErrorCode() : failure instanceof SQLException sql ? sql.getErrorCode() : 0;
	}

	private static List<Throwable> exceptionGraph(Throwable failure) {
		final List<Throwable> result = new ArrayList<>();
		collect( failure, Collections.newSetFromMap( new IdentityHashMap<>() ), result );
		return result;
	}

	private static void collect(Throwable failure, Set<Throwable> seen, List<Throwable> result) {
		if ( failure == null || !seen.add( failure ) ) { return; }
		result.add( failure );
		collect( failure.getCause(), seen, result );
		for ( var suppressed : failure.getSuppressed() ) { collect( suppressed, seen, result ); }
	}

	private static LockTimeoutConnectionProvider provider(SessionFactoryScope scope) {
		return (LockTimeoutConnectionProvider) scope.getSessionFactory().getServiceRegistry().getService( ConnectionProvider.class );
	}

	@Entity(name = "ProductionLifecycleItem")
	@Table(name = "timeout_lifecycle_item")
	static class Item {
		@Id int id;
		int marker;
		Item() {}
		Item(int id) { this.id = id; }
	}
}
