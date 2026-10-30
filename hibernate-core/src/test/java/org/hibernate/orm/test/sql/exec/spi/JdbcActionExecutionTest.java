package org.hibernate.orm.test.sql.exec.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.spi.JdbcActionExecution;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.exec.spi.PostAction;
import org.hibernate.sql.exec.spi.PreAction;
import org.hibernate.sql.exec.spi.SecondaryAction;
import org.hibernate.sql.exec.spi.StatementAccess;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Contracts for invocation ownership, materialization, and ordered cleanup.
///
/// @author Steve Ebersole
class JdbcActionExecutionTest {
	@Test
	void pairsByIdentityAndCreatesFreshInstances() {
		final var first = new Definition();
		final var equalButDistinct = new Definition();
		final var a = JdbcActionExecution.fromActions(
				new PreAction[] { first, equalButDistinct }, new PostAction[] { equalButDistinct, first } );
		final var b = JdbcActionExecution.fromActions(
				new PreAction[] { first, equalButDistinct }, new PostAction[] { equalButDistinct, first } );
		assertThat( first.created ).hasSize( 2 );
		assertThat( equalButDistinct.created ).hasSize( 2 );
		a.performPreActions( null, null, null );
		b.performPreActions( null, null, null );
		a.performPostActions( true, null, null, null, null );
		b.performPostActions( true, null, null, null, null );
		assertThat( first.created ).allSatisfy( instance -> assertThat( instance.finished ).isTrue() );
		assertThat( equalButDistinct.created ).allSatisfy( instance -> assertThat( instance.finished ).isTrue() );
	}

	@Test
	void asynchronousIntegrationsCanMaterializePairedInstancesWithoutCallbacks() {
		final var definition = new Definition();
		final var instances = JdbcActionExecution.materializeActions(
				new PreAction[] { definition }, new PostAction[] { definition } );
		assertThat( instances.preActions().get( 0 ) ).isSameAs( instances.postActions().get( 0 ) );
		assertThat( definition.created.get( 0 ).started ).isFalse();
		assertThatThrownBy( () -> instances.preActions().clear() ).isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( () -> instances.postActions().clear() ).isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	void statelessActionsKeepTheirOrderAndRunOnlyOnce() {
		final List<String> events = new ArrayList<>();
		final PreAction pre = (a, c, e) -> events.add( "pre" );
		assertThat( pre.createExecutionAction() ).isSameAs( pre );
		final var execution = JdbcActionExecution.fromActions( new PreAction[] { pre },
				new PostAction[] { post( () -> events.add( "restore" ), false ), post( () -> events.add( "follow-on" ), false ) } );
		execution.performPreActions( null, null, null );
		execution.performPostActions( true, null, null, null, null );
		execution.performPostActions( false, null, null, null, null );
		assertThat( events ).containsExactly( "pre", "restore", "follow-on" );
		assertThatThrownBy( () -> execution.performPreActions( null, null, null ) ).isInstanceOf( IllegalStateException.class );
	}

	@Test
	void noActionsUseSharedNoOp() {
		assertThat( JdbcActionExecution.fromActions( null, null ) ).isSameAs( JdbcActionExecution.NONE );
		assertThat( JdbcActionExecution.fromActions( new PreAction[0], new PostAction[0] ) ).isSameAs( JdbcActionExecution.NONE );
	}

	@Test
	void rejectsNullAndIncompatibleInstances() {
		final PreAction nullDefinition = new PreAction() {
			@Override public SecondaryAction createExecutionAction() { return null; }
			@Override public void performPreAction(StatementAccess a, java.sql.Connection c, ExecutionContext e) { fail(); }
		};
		final PreAction wrongRole = new PreAction() {
			@Override public SecondaryAction createExecutionAction() { return post( () -> fail(), false ); }
			@Override public void performPreAction(StatementAccess a, java.sql.Connection c, ExecutionContext e) { fail(); }
		};
		assertThatThrownBy( () -> JdbcActionExecution.fromActions( new PreAction[] { nullDefinition }, null ) )
				.isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> JdbcActionExecution.fromActions( new PreAction[] { wrongRole }, null ) )
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void rejectsDuplicateDefinitionsWithinEitherPhase() {
		final var definition = new Definition();
		assertThatThrownBy( () -> JdbcActionExecution.fromActions( new PreAction[] { definition, definition }, null ) )
				.isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> JdbcActionExecution.fromActions( null, new PostAction[] { definition, definition } ) )
				.isInstanceOf( IllegalArgumentException.class );
		assertThat( definition.created ).allSatisfy( instance -> assertThat( instance.started ).isFalse() );
	}

	@Test
	void failureSkipsSuccessOnlyWorkAndPreservesSuppressionOrder() {
		final var original = new IllegalStateException( "primary" );
		final var first = new IllegalArgumentException( "first cleanup" );
		final var second = new AssertionError( "second cleanup" );
		final List<String> events = new ArrayList<>();
		final var execution = JdbcActionExecution.fromActions( null, new PostAction[] {
				post( () -> fail( "success-only action" ), false ),
				post( () -> { events.add( "first" ); throw first; }, true ),
				post( () -> { events.add( "second" ); throw second; }, true ),
				post( () -> { throw original; }, true ),
				post( () -> events.add( "last" ), true )
		} );
		execution.cleanupAfterFailure( original, null, null, null, null );
		execution.cleanupAfterFailure( original, null, null, null, null );
		assertThat( original.getSuppressed() ).containsExactly( first, second );
		assertThat( events ).containsExactly( "first", "second", "last" );
	}

	@Test
	void laterPostFailureDoesNotRepeatRestoration() {
		final List<String> events = new ArrayList<>();
		final var original = new IllegalStateException( "follow-on" );
		final var cleanup = new IllegalArgumentException( "cleanup" );
		final var execution = JdbcActionExecution.fromActions( null, new PostAction[] {
				post( () -> events.add( "restore" ), true ),
				post( () -> { events.add( "follow-on" ); throw original; }, false ),
				post( () -> fail( "remaining success-only action" ), false ),
				post( () -> { events.add( "cleanup" ); throw cleanup; }, true )
		} );
		assertThatThrownBy( () -> execution.performPostActions( true, null, null, null, null ) ).isSameAs( original );
		execution.cleanupAfterFailure( original, null, null, null, null );
		assertThat( original.getSuppressed() ).containsExactly( cleanup );
		assertThat( events ).containsExactly( "restore", "follow-on", "cleanup" );
	}

	@Test
	void failedEligibilityDoesNotPreventLaterCleanup() {
		final var original = new AssertionError( "primary" );
		final var eligibilityFailure = new IllegalStateException( "eligibility" );
		final List<String> events = new ArrayList<>();
		final PostAction broken = new PostAction() {
			@Override public boolean shouldRunAfterFail() { throw eligibilityFailure; }
			@Override public void performPostAction(StatementAccess a, java.sql.Connection c, ExecutionContext e, LoadedValuesCollector l) { fail(); }
		};
		final var execution = JdbcActionExecution.fromActions( null,
				new PostAction[] { broken, post( () -> events.add( "cleanup" ), true ) } );
		execution.cleanupAfterFailure( original, null, null, null, null );
		assertThat( original.getSuppressed() ).containsExactly( eligibilityFailure );
		assertThat( events ).containsExactly( "cleanup" );
	}

	@Test
	void legacyAdapterInvokesCallbacksOnce() {
		final var select = mock( JdbcSelect.class, CALLS_REAL_METHODS );
		final var execution = select.createActionExecution();
		execution.performPreActions( null, null, null );
		execution.performPostActions( true, null, null, null, null );
		execution.performPostActions( false, null, null, null, null );
		verify( select ).performPreActions( null, null, null );
		verify( select, times( 1 ) ).performPostActions( true, null, null, null, null );
		assertThatThrownBy( () -> execution.performPreActions( null, null, null ) ).isInstanceOf( IllegalStateException.class );
	}

	@Test
	void subclassCustomizesInvocationLocalRunners() {
		final var definition = new Definition();
		final List<String> events = new ArrayList<>();
		final var select = new CustomizingSelect( definition, events );
		final var first = select.createActionExecution();
		final var second = select.createActionExecution();
		assertThat( first ).isNotSameAs( second );
		assertThat( definition.created ).hasSize( 2 );

		first.performPreActions( null, null, null );
		second.performPreActions( null, null, null );
		assertThat( definition.created ).allSatisfy( instance -> assertThat( instance.started ).isTrue() );
		first.performPostActions( true, null, null, null, null );
		assertThat( definition.created.get( 0 ).finished ).isTrue();
		assertThat( definition.created.get( 1 ).finished ).isFalse();

		final var original = new IllegalStateException( "second invocation" );
		second.cleanupAfterFailure( original, null, null, null, null );
		second.cleanupAfterFailure( original, null, null, null, null );
		assertThat( definition.created.get( 1 ).finished ).isTrue();
		assertThat( original.getSuppressed() ).isEmpty();
		assertThat( events ).containsExactly( "custom pre", "custom pre", "custom post", "custom post" );
	}

	@Test
	void executorUsesSubclassRunnerAndDelegatesFailureCleanup() {
		final var original = new IllegalStateException( "result-set setup" );
		final var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		final var context = mock( ExecutionContext.class );
		when( context.getSession() ).thenReturn( session );
		when( context.getQueryOptions() ).thenReturn( mock( QueryOptions.class ) );
		when( session.getJdbcServices() ).thenThrow( original );
		final var definition = new Definition();
		final List<String> events = new ArrayList<>();
		final var select = new CustomizingSelect( definition, events );

		assertThatThrownBy( () -> JdbcSelectExecutorStandardImpl.INSTANCE.executeQuery(
				select, null, context, null, Object[].class, null, null ) ).isSameAs( original );
		assertThat( definition.created ).singleElement().satisfies( instance -> {
			assertThat( instance.started ).isTrue();
			assertThat( instance.finished ).isTrue();
		} );
		assertThat( events ).containsExactly( "custom pre", "custom post" );
		assertThat( original.getSuppressed() ).isEmpty();
	}

	@Test
	void executorCleansUpWhenSetupFailsAfterPreActions() {
		final var original = new IllegalStateException( "result-set setup" );
		final var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		final var context = mock( ExecutionContext.class );
		when( context.getSession() ).thenReturn( session );
		when( context.getQueryOptions() ).thenReturn( mock( QueryOptions.class ) );
		when( session.getJdbcServices() ).thenThrow( original );
		final List<String> events = new ArrayList<>();
		final var runner = JdbcActionExecution.fromActions(
				new PreAction[] { (a, c, e) -> events.add( "apply" ) },
				new PostAction[] { post( () -> events.add( "restore" ), true ) } );
		final var select = mock( JdbcSelect.class );
		when( select.createActionExecution() ).thenReturn( runner );
		assertThatThrownBy( () -> JdbcSelectExecutorStandardImpl.INSTANCE.executeQuery(
				select, null, context, null, Object[].class, null, null ) ).isSameAs( original );
		assertThat( events ).containsExactly( "apply", "restore" );
		verify( select ).createActionExecution();
	}

	@Test
	void executorCleansUpOnPreActionErrorAndPreservesOriginal() {
		final var original = new AssertionError( "later pre-action" );
		final var cleanup = new IllegalStateException( "restore" );
		final var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		final var context = mock( ExecutionContext.class );
		when( context.getSession() ).thenReturn( session );
		when( context.getQueryOptions() ).thenReturn( mock( QueryOptions.class ) );
		final List<String> events = new ArrayList<>();
		final var runner = JdbcActionExecution.fromActions(
				new PreAction[] { (a, c, e) -> events.add( "apply" ), (a, c, e) -> { throw original; } },
				new PostAction[] { post( () -> { events.add( "restore" ); throw cleanup; }, true ) } );
		final var select = mock( JdbcSelect.class );
		when( select.createActionExecution() ).thenReturn( runner );
		assertThatThrownBy( () -> JdbcSelectExecutorStandardImpl.INSTANCE.executeQuery(
				select, null, context, null, Object[].class, null, null ) ).isSameAs( original );
		assertThat( original.getSuppressed() ).containsExactly( cleanup );
		assertThat( events ).containsExactly( "apply", "restore" );
	}

	private static PostAction post(Runnable work, boolean afterFailure) {
		return new PostAction() {
			@Override public boolean shouldRunAfterFail() { return afterFailure; }
			@Override public void performPostAction(StatementAccess a, java.sql.Connection c, ExecutionContext e, LoadedValuesCollector l) { work.run(); }
		};
	}

	private static class CustomizingSelect extends JdbcSelectWithActions {
		private final List<String> events;

		private CustomizingSelect(Definition definition, List<String> events) {
			super( mock( JdbcOperationQuerySelect.class ), null,
					new PreAction[] { definition }, new PostAction[] { definition } );
			this.events = events;
		}

		@Override
		public JdbcActionExecution createActionExecution() {
			final var delegate = super.createActionExecution();
			final var customization = JdbcActionExecution.fromActions(
					new PreAction[] { (a, c, e) -> events.add( "custom pre" ) },
					new PostAction[] { post( () -> events.add( "custom post" ), true ) } );
			return new JdbcActionExecution() {
				@Override
				public void performPreActions(StatementAccess a, java.sql.Connection c, ExecutionContext e) {
					customization.performPreActions( a, c, e );
					delegate.performPreActions( a, c, e );
				}

				@Override
				public void performPostActions(boolean succeeded, StatementAccess a, java.sql.Connection c,
						ExecutionContext e, LoadedValuesCollector l) {
					delegate.performPostActions( succeeded, a, c, e, l );
					customization.performPostActions( succeeded, a, c, e, l );
				}

				@Override
				public void cleanupAfterFailure(Throwable failure, StatementAccess a, java.sql.Connection c,
						ExecutionContext e, LoadedValuesCollector l) {
					delegate.cleanupAfterFailure( failure, a, c, e, l );
					customization.cleanupAfterFailure( failure, a, c, e, l );
				}
			};
		}

		@Override
		public void performPreActions(StatementAccess a, java.sql.Connection c, ExecutionContext e) {
			fail( "The executor must use the subclass runner instead of legacy callbacks" );
		}

		@Override
		public void performPostActions(boolean succeeded, StatementAccess a, java.sql.Connection c,
				ExecutionContext e, LoadedValuesCollector l) {
			fail( "The executor must use the subclass runner instead of legacy callbacks" );
		}
	}

	private static class Definition implements PreAction, PostAction {
		private final List<Bound> created = new ArrayList<>();
		@Override public SecondaryAction createExecutionAction() {
			final var instance = new Bound();
			created.add( instance );
			return instance;
		}
		@Override public boolean equals(Object other) { return other instanceof Definition; }
		@Override public int hashCode() { return 1; }
		@Override public void performPreAction(StatementAccess a, java.sql.Connection c, ExecutionContext e) { fail( "definition invoked" ); }
		@Override public void performPostAction(StatementAccess a, java.sql.Connection c, ExecutionContext e, LoadedValuesCollector l) { fail( "definition invoked" ); }
	}

	private static class Bound implements PreAction, PostAction {
		private boolean started;
		private boolean finished;
		@Override public boolean shouldRunAfterFail() { return started && !finished; }
		@Override public void performPreAction(StatementAccess a, java.sql.Connection c, ExecutionContext e) {
			assertThat( started ).isFalse();
			started = true;
		}
		@Override public void performPostAction(StatementAccess a, java.sql.Connection c, ExecutionContext e, LoadedValuesCollector l) {
			assertThat( started ).isTrue();
			assertThat( finished ).isFalse();
			finished = true;
		}
	}
}
