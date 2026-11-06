package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;

import org.hibernate.Session;
import org.hibernate.Locking;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.lock.internal.H2LockingSupport;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.JdbcSettings.DIALECT;

/// Exercises cached query execution with real H2 connection timeout commands.
/// The test-only dialect routes timeouts through connection settings; this does
/// not change H2's production locking capability profile.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = LockTimeoutExecutionIntegrationTest.Item.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = DIALECT,
		value = "org.hibernate.orm.test.locking.LockTimeoutExecutionIntegrationTest$ConnectionTimeoutDialect"))
@Jira("https://hibernate.atlassian.net/browse/HHH-21032")
class LockTimeoutExecutionIntegrationTest {
	private static final String HQL = "from TimeoutExecutionItem where id = :id";

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1 ) );
			session.persist( new Item( 2 ) );
		} );
		// Populate the shared interpretation before the two executions overlap.
		scope.inTransaction( session -> query( session, 1 ) );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void overlappingCachedQueriesRestoreActualConnectionSettings(SessionFactoryScope scope) throws Exception {
		final var firstInQuery = new CountDownLatch( 1 );
		final var secondInQuery = new CountDownLatch( 1 );
		final var firstCompleted = new CountDownLatch( 1 );
		final var executor = Executors.newFixedThreadPool( 2 );
		try {
			final var first = executor.submit( () -> {
				try {
					execute( scope, 1, 1000, sql -> {
						firstInQuery.countDown();
						await( secondInQuery );
						return sql;
					} );
				}
				finally {
					firstInQuery.countDown();
					firstCompleted.countDown();
				}
			} );
			final var second = executor.submit( () -> {
				try {
					await( firstInQuery );
					execute( scope, 2, 2000, sql -> {
						secondInQuery.countDown();
						await( firstCompleted );
						return sql;
					} );
				}
				finally {
					secondInQuery.countDown();
				}
			} );
			first.get( 30, TimeUnit.SECONDS );
			second.get( 30, TimeUnit.SECONDS );
		}
		finally {
			executor.shutdownNow();
			assertThat( executor.awaitTermination( 30, TimeUnit.SECONDS ) ).isTrue();
		}
	}

	@Test
	void statementPreparationFailureRestoresActualConnectionSetting(SessionFactoryScope scope) {
		final var original = new IllegalStateException( "statement preparation" );
		try ( var session = scope.getSessionFactory().withOptions()
				.statementInspector( (UnaryOperator<String>) sql -> { throw original; } ).openSession() ) {
			final var transaction = session.beginTransaction();
			final int baseline = session.doReturningWork( LockTimeoutExecutionIntegrationTest::readTimeout );
			try {
				assertThatThrownBy( () -> query( session, 1 ) ).isSameAs( original );
				assertThat( session.doReturningWork( LockTimeoutExecutionIntegrationTest::readTimeout ) ).isEqualTo( baseline );
			}
			finally {
				session.doWork( connection -> setTimeout( connection, baseline ) );
				transaction.rollback();
			}
		}
	}

	@Test
	void resultConsumptionFailureRestoresActualConnectionSetting(SessionFactoryScope scope) {
		final var original = new IllegalStateException( "row transformation" );
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			final int originalTimeout = session.doReturningWork( LockTimeoutExecutionIntegrationTest::readTimeout );
			try {
				session.doWork( connection -> setTimeout( connection, 1000 ) );
				assertThatThrownBy( () -> session.createQuery( HQL, Item.class ).setParameter( "id", 1 )
						.setLockMode( PESSIMISTIC_WRITE ).setLockTimeout( Timeout.milliseconds( 500 ) )
						.setTupleTransformer( (tuple, aliases) -> {
							assertThat( ( (Item) tuple[0] ).id ).isEqualTo( 1 );
							assertThat( session.doReturningWork( LockTimeoutExecutionIntegrationTest::readTimeout ) )
									.as( "timeout active while consuming a returned row" ).isEqualTo( 500 );
							throw original;
						} ).getSingleResult() ).isSameAs( original );
				assertThat( session.doReturningWork( LockTimeoutExecutionIntegrationTest::readTimeout ) ).isEqualTo( 1000 );
				assertThat( original.getSuppressed() ).isEmpty();
			}
			finally {
				session.doWork( connection -> setTimeout( connection, originalTimeout ) );
				transaction.rollback();
			}
		}
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21027")
	void missingFollowOnRowRestoresActualConnectionSetting(SessionFactoryScope scope) {
		final var selects = new AtomicInteger();
		final UnaryOperator<String> inspector = sql -> {
			if ( sql.startsWith( "select " ) && sql.contains( "timeout_execution_item" )
					&& selects.incrementAndGet() == 2 ) {
				scope.inTransaction( writer -> assertThat( writer.createMutationQuery( "delete from TimeoutExecutionItem where id = 1" )
						.executeUpdate() ).isEqualTo( 1 ) );
			}
			return sql;
		};
		try ( var session = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			final var transaction = session.beginTransaction();
			final int originalTimeout = session.doReturningWork( LockTimeoutExecutionIntegrationTest::readTimeout );
			try {
				session.doWork( connection -> setTimeout( connection, 1000 ) );
				assertThatThrownBy( () -> session.createQuery( HQL, Item.class ).setParameter( "id", 1 )
						.setLockMode( PESSIMISTIC_WRITE ).setLockTimeout( Timeout.milliseconds( 500 ) )
						.setFollowOnStrategy( Locking.FollowOn.FORCE ).getSingleResult() )
						.isInstanceOf( OptimisticLockException.class ).hasCauseInstanceOf( StaleObjectStateException.class );
				assertThat( selects.get() ).isEqualTo( 2 );
				assertThat( session.doReturningWork( LockTimeoutExecutionIntegrationTest::readTimeout ) ).isEqualTo( 1000 );
			}
			finally {
				session.doWork( connection -> setTimeout( connection, originalTimeout ) );
				transaction.rollback();
			}
		}
	}

	private static void execute(SessionFactoryScope scope, int id, int baseline, UnaryOperator<String> inspector) {
		try ( var session = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			final var transaction = session.beginTransaction();
			final int original = session.doReturningWork( LockTimeoutExecutionIntegrationTest::readTimeout );
			try {
				session.doWork( connection -> setTimeout( connection, baseline ) );
				query( session, id );
				assertThat( session.doReturningWork( LockTimeoutExecutionIntegrationTest::readTimeout ) )
						.as( "original timeout of execution %s", id ).isEqualTo( baseline );
			}
			finally {
				try {
					session.doWork( connection -> setTimeout( connection, original ) );
				}
				finally {
					transaction.rollback();
				}
			}
		}
	}

	private static void query(Session session, int id) {
		assertThat( session.createQuery( HQL, Item.class ).setParameter( "id", id )
				.setLockMode( PESSIMISTIC_WRITE ).setLockTimeout( Timeout.milliseconds( 500 ) )
				.getSingleResult().id ).isEqualTo( id );
	}

	private static void await(CountDownLatch latch) {
		try {
			assertThat( latch.await( 10, TimeUnit.SECONDS ) ).as( "execution interleaving reached" ).isTrue();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException( e );
		}
	}

	private static int readTimeout(Connection connection) throws SQLException {
		try ( var statement = connection.createStatement(); var result = statement.executeQuery( "call lock_timeout()" ) ) {
			result.next();
			return result.getInt( 1 );
		}
	}

	private static void setTimeout(Connection connection, int timeout) throws SQLException {
		try ( var statement = connection.createStatement() ) {
			statement.executeUpdate( "set lock_timeout " + timeout );
		}
	}

	public static class ConnectionTimeoutDialect extends H2Dialect {
		@Override
		public LockingSupport getLockingSupport() {
			return new ConnectionTimeoutSupport();
		}
	}

	private static class ConnectionTimeoutSupport implements LockingSupport, LockingSupport.Metadata, ConnectionLockTimeoutStrategy {
		@Override public Metadata getMetadata() { return this; }
		@Override public OuterJoinLockingType getOuterJoinLockingType() { return OuterJoinLockingType.IGNORED; }
		@Override public LockTimeoutType getLockTimeoutType(Timeout timeout) { return LockTimeoutType.CONNECTION; }
		@Override public LockingClauseRenderer getLockingClauseRenderer() { return request -> " for update"; }
		@Override public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() { return this; }
		@Override public TransactionConcurrencyResolver getTransactionConcurrencyResolver() {
			return H2LockingSupport.INSTANCE.getTransactionConcurrencyResolver();
		}
		@Override public Level getSupportedLevel() { return Level.SUPPORTED; }
		@Override public Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
			try { return Timeout.milliseconds( readTimeout( connection ) ); }
			catch (SQLException e) { throw factory.getJdbcServices().getSqlExceptionHelper().convert( e, "read test timeout" ); }
		}
		@Override public void setLockTimeout(Timeout timeout, Connection connection, SessionFactoryImplementor factory) {
			try { setTimeout( connection, timeout.milliseconds() ); }
			catch (SQLException e) { throw factory.getJdbcServices().getSqlExceptionHelper().convert( e, "set test timeout" ); }
		}
	}

	@Entity(name = "TimeoutExecutionItem")
	@Table(name = "timeout_execution_item")
	static class Item {
		@Id int id;
		Item() {}
		Item(int id) { this.id = id; }
	}
}
