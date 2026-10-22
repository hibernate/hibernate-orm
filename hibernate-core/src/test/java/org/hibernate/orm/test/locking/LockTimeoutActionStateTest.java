package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.util.IdentityHashMap;
import java.util.Map;

import jakarta.persistence.Timeout;

import org.hibernate.LockOptions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Reuses one JDBC select and its real timeout action as a cached plan would.
/// Two execution contexts and simulated connection settings expose overlapping
/// execution deterministically, without threads, sleeps, or a database dependency.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@Tag("db-locking")
@Jira( "https://hibernate.atlassian.net/browse/HHH-21032" )
class LockTimeoutActionStateTest {

	@Test
	void sequentialExecutionsRestoreTheirOwnTimeouts() {
		final var fixture = new Fixture();
		fixture.startFirst();
		assertThat( fixture.settings.timeout( fixture.firstConnection ) ).isEqualTo( 500 );
		fixture.finishFirst();
		assertThat( fixture.settings.timeout( fixture.firstConnection ) ).isEqualTo( 1000 );

		fixture.startSecond();
		assertThat( fixture.settings.timeout( fixture.secondConnection ) ).isEqualTo( 500 );
		fixture.finishSecond();
		fixture.assertOriginalTimeouts();
	}

	@ParameterizedTest(name = "second execution finishes first = {0}")
	@ValueSource(booleans = { false, true })
	@FailureExpected(reason = "HHH-21032 reproducer")
	void overlappingExecutionsRestoreTheirOwnTimeouts(boolean secondFinishesFirst) {
		final var fixture = new Fixture();

		// Both executions share the exact same operation and timeout action.
		fixture.startFirst();
		fixture.startSecond();
		assertThat( fixture.settings.timeout( fixture.firstConnection ) ).isEqualTo( 500 );
		assertThat( fixture.settings.timeout( fixture.secondConnection ) ).isEqualTo( 500 );

		if ( secondFinishesFirst ) {
			fixture.finishSecond();
			fixture.finishFirst();
		}
		else {
			fixture.finishFirst();
			fixture.finishSecond();
		}
		fixture.assertOriginalTimeouts();
	}

	private static class Fixture {
		private final Connection firstConnection = mock( Connection.class, "first connection" );
		private final Connection secondConnection = mock( Connection.class, "second connection" );
		private final ExecutionContext firstContext;
		private final ExecutionContext secondContext;
		private final ConnectionSettings settings = new ConnectionSettings();
		private final JdbcSelect operation;

		private Fixture() {
			final var factory = mock( SessionFactoryImplementor.class );
			firstContext = executionContext( factory );
			secondContext = executionContext( factory );
			settings.timeouts.put( firstConnection, Timeout.milliseconds( 1000 ) );
			settings.timeouts.put( secondConnection, Timeout.milliseconds( 2000 ) );

			final var lockingSupport = mock( LockingSupport.class );
			when( lockingSupport.getConnectionLockTimeoutStrategy() ).thenReturn( settings );
			operation = new JdbcSelectWithActions.Builder()
					.setPrimaryAction( mock( JdbcOperationQuerySelect.class ) )
					.setLockTimeoutType( LockTimeoutType.CONNECTION )
					.setLockingSupport( lockingSupport )
					.setLockOptions( new LockOptions( PESSIMISTIC_WRITE ).setTimeout( Timeout.milliseconds( 500 ) ) )
					.build();
			assertThat( operation ).isInstanceOf( JdbcSelectWithActions.class );
		}

		private static ExecutionContext executionContext(SessionFactoryImplementor factory) {
			final var session = mock( SharedSessionContractImplementor.class );
			when( session.getFactory() ).thenReturn( factory );
			final var context = mock( ExecutionContext.class );
			when( context.getSession() ).thenReturn( session );
			return context;
		}

		private void startFirst() {
			operation.performPreActions( null, firstConnection, firstContext );
		}

		private void startSecond() {
			operation.performPreActions( null, secondConnection, secondContext );
		}

		private void finishFirst() {
			operation.performPostActions( true, null, firstConnection, firstContext, null );
		}

		private void finishSecond() {
			operation.performPostActions( true, null, secondConnection, secondContext, null );
		}

		private void assertOriginalTimeouts() {
			assertSoftly( assertions -> {
				assertions.assertThat( settings.timeout( firstConnection ) )
						.as( "first connection's original timeout" ).isEqualTo( 1000 );
				assertions.assertThat( settings.timeout( secondConnection ) )
						.as( "second connection's original timeout" ).isEqualTo( 2000 );
			} );
		}
	}

	private static class ConnectionSettings implements ConnectionLockTimeoutStrategy {
		private final Map<Connection, Timeout> timeouts = new IdentityHashMap<>();

		@Override
		public Level getSupportedLevel() {
			return Level.SUPPORTED;
		}

		@Override
		public Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
			return timeouts.get( connection );
		}

		@Override
		public void setLockTimeout(Timeout timeout, Connection connection, SessionFactoryImplementor factory) {
			timeouts.put( connection, timeout );
		}

		private int timeout(Connection connection) {
			return timeouts.get( connection ).milliseconds();
		}
	}
}
