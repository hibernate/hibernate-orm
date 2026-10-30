package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.util.IdentityHashMap;
import java.util.Map;

import jakarta.persistence.Timeout;

import org.hibernate.LockOptions;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.internal.LockTimeoutHandler;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcActionExecution;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Reuses one JDBC select and its real timeout action as a cached plan would.
/// Two execution contexts and simulated connection settings expose overlapping
/// execution deterministically, without threads, sleeps, or a database dependency.
///
/// @author Steve Ebersole
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

	@Test
	void nestedExecutionRestoresOuterSettingAndThenOriginal() {
		final var fixture = new Fixture();
		fixture.startFirst();
		final var definition = new LockTimeoutHandler( Timeout.milliseconds( 250 ), fixture.settings );
		final var nested = JdbcActionExecution.fromActions(
				new org.hibernate.sql.exec.spi.PreAction[] { definition },
				new org.hibernate.sql.exec.spi.PostAction[] { definition } );
		nested.performPreActions( null, fixture.firstConnection, fixture.firstContext );
		assertThat( fixture.settings.timeout( fixture.firstConnection ) ).isEqualTo( 250 );
		nested.performPostActions( true, null, fixture.firstConnection, fixture.firstContext, null );
		assertThat( fixture.settings.timeout( fixture.firstConnection ) ).isEqualTo( 500 );
		fixture.finishFirst();
		fixture.assertOriginalTimeouts();
		final var next = fixture.operation.createActionExecution();
		next.performPreActions( null, fixture.firstConnection, fixture.firstContext );
		next.performPostActions( true, null, fixture.firstConnection, fixture.firstContext, null );
		fixture.assertOriginalTimeouts();
	}

	@Test
	void failedBaselineReadDoesNotApplyOrRestore() {
		final var fixture = new Fixture();
		final var original = new IllegalStateException( "read" );
		fixture.settings.readFailure = original;
		assertThatThrownBy( fixture::startFirst ).isSameAs( original );
		fixture.firstExecution.cleanupAfterFailure( original, null, fixture.firstConnection, fixture.firstContext, null );
		assertThat( fixture.settings.writes ).isZero();
		assertThat( original.getSuppressed() ).isEmpty();
	}

	@Test
	void failedSetterThatChangedSettingIsRestored() {
		final var fixture = new Fixture();
		final var original = new IllegalStateException( "apply after mutation" );
		fixture.settings.applyFailure = original;
		assertThatThrownBy( fixture::startFirst ).isSameAs( original );
		assertThat( fixture.settings.timeout( fixture.firstConnection ) ).isEqualTo( 500 );
		fixture.firstExecution.cleanupAfterFailure( original, null, fixture.firstConnection, fixture.firstContext, null );
		fixture.assertOriginalTimeouts();
		assertThat( fixture.settings.writes ).isEqualTo( 2 );
	}

	@Test
	void failedRestoreIsSuppressedAndNeverRetried() {
		final var fixture = new Fixture();
		fixture.startFirst();
		final var original = new AssertionError( "primary" );
		final var restoreFailure = new IllegalStateException( "restore" );
		fixture.settings.restoreFailure = restoreFailure;
		fixture.firstExecution.cleanupAfterFailure( original, null, fixture.firstConnection, fixture.firstContext, null );
		fixture.firstExecution.cleanupAfterFailure( original, null, fixture.firstConnection, fixture.firstContext, null );
		assertThat( original.getSuppressed() ).containsExactly( restoreFailure );
		assertThat( fixture.settings.writes ).isEqualTo( 2 );
	}

	@Test
	void restoreFailureOnSuccessIsPropagated() {
		final var fixture = new Fixture();
		fixture.startFirst();
		final var restoreFailure = new IllegalStateException( "restore" );
		fixture.settings.restoreFailure = restoreFailure;
		assertThatThrownBy( fixture::finishFirst ).isSameAs( restoreFailure );
		fixture.firstExecution.cleanupAfterFailure( restoreFailure, null, fixture.firstConnection, fixture.firstContext, null );
		assertThat( fixture.settings.writes ).isEqualTo( 2 );
		assertThat( restoreFailure.getSuppressed() ).isEmpty();
	}

	@Test
	void cachedDefinitionCannotBeInvokedDirectly() {
		final var fixture = new Fixture();
		assertThatThrownBy( () -> fixture.operation.performPreActions( null, fixture.firstConnection, fixture.firstContext ) )
				.isInstanceOf( IllegalStateException.class );
		assertThatThrownBy( () -> fixture.operation.performPostActions( true, null, fixture.firstConnection, fixture.firstContext, null ) )
				.isInstanceOf( IllegalStateException.class );
		assertThatThrownBy( () -> fixture.operation.performPostActions( false, null, fixture.firstConnection, fixture.firstContext, null ) )
				.isInstanceOf( IllegalStateException.class );
		assertThat( fixture.settings.writes ).isZero();
	}

	private static class Fixture {
		private final Connection firstConnection = mock( Connection.class, "first connection" );
		private final Connection secondConnection = mock( Connection.class, "second connection" );
		private final ExecutionContext firstContext;
		private final ExecutionContext secondContext;
		private final ConnectionSettings settings = new ConnectionSettings();
		private final JdbcSelect operation;
		private final JdbcActionExecution firstExecution;
		private final JdbcActionExecution secondExecution;

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
			firstExecution = operation.createActionExecution();
			secondExecution = operation.createActionExecution();
		}

		private static ExecutionContext executionContext(SessionFactoryImplementor factory) {
			final var session = mock( SharedSessionContractImplementor.class );
			when( session.getFactory() ).thenReturn( factory );
			final var context = mock( ExecutionContext.class );
			when( context.getSession() ).thenReturn( session );
			return context;
		}

		private void startFirst() {
			firstExecution.performPreActions( null, firstConnection, firstContext );
		}

		private void startSecond() {
			secondExecution.performPreActions( null, secondConnection, secondContext );
		}

		private void finishFirst() {
			firstExecution.performPostActions( true, null, firstConnection, firstContext, null );
		}

		private void finishSecond() {
			secondExecution.performPostActions( true, null, secondConnection, secondContext, null );
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
		private RuntimeException readFailure;
		private RuntimeException applyFailure;
		private RuntimeException restoreFailure;
		private int writes;

		@Override
		public Level getSupportedLevel() {
			return Level.SUPPORTED;
		}

		@Override
		public Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
			if ( readFailure != null ) {
				throw readFailure;
			}
			return timeouts.get( connection );
		}

		@Override
		public void setLockTimeout(Timeout timeout, Connection connection, SessionFactoryImplementor factory) {
			writes++;
			if ( timeout.milliseconds() != 500 && restoreFailure != null ) {
				throw restoreFailure;
			}
			timeouts.put( connection, timeout );
			if ( timeout.milliseconds() == 500 && applyFailure != null ) {
				throw applyFailure;
			}
		}

		private int timeout(Connection connection) {
			return timeouts.get( connection ).milliseconds();
		}
	}
}
