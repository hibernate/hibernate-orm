package org.hibernate.orm.test.sql.exec.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcActionExecution;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.ResultsHelper;
import org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess;
import org.hibernate.sql.results.jdbc.internal.ResultSetAccess;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/// Failure cleanup retains the primary exception and releases only the
/// invocation's result-set access, even with a custom action runner.
///
/// @author Steve Ebersole
class JdbcSelectFailureCleanupTest {
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void setupFailureReleasesAccessAfterActionCleanup(boolean error) {
		final var original = failure( error, "metadata setup" );
		final List<String> events = new ArrayList<>();
		final var runner = mock( JdbcActionExecution.class );
		doAnswer( invocation -> { events.add( "actions" ); return null; } ).when( runner )
				.cleanupAfterFailure( same( original ), any(), any(), any(), isNull() );
		try ( var accesses = mockConstruction( DeferredResultSetAccess.class, (access, context) ->
				doAnswer( invocation -> { events.add( "release" ); return null; } ).when( access ).release() ) ) {
			executeFailingSetup( original, runner );
			assertThat( accesses.constructed() ).hasSize( 1 );
			assertThat( events ).containsExactly( "actions", "release" );
			assertThat( original.getSuppressed() ).isEmpty();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void brokenCustomCleanupDoesNotPreventRelease(boolean error) {
		final var original = failure( error, "metadata setup" );
		final var actionFailure = failure( !error, "custom cleanup" );
		final var releaseFailure = failure( error, "JDBC release" );
		final var runner = mock( JdbcActionExecution.class );
		doThrow( actionFailure ).when( runner ).cleanupAfterFailure(
				same( original ), any(), any(), any(), isNull() );
		try ( var accesses = mockConstruction( DeferredResultSetAccess.class, (access, context) ->
				doThrow( releaseFailure ).when( access ).release() ) ) {
			executeFailingSetup( original, runner );
			assertThat( accesses.constructed() ).hasSize( 1 );
			assertThat( original.getSuppressed() ).containsExactly( actionFailure, releaseFailure );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void cleanupDoesNotSuppressPrimaryOntoItself(boolean error) {
		final var original = failure( error, "metadata setup" );
		final var runner = mock( JdbcActionExecution.class );
		doThrow( original ).when( runner ).cleanupAfterFailure(
				same( original ), any(), any(), any(), isNull() );
		try ( var accesses = mockConstruction( DeferredResultSetAccess.class, (access, context) ->
				doThrow( original ).when( access ).release() ) ) {
			executeFailingSetup( original, runner );
			assertThat( accesses.constructed() ).hasSize( 1 );
			assertThat( original.getSuppressed() ).isEmpty();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "unreleased consumer", "released consumer", "post action" })
	void executorDoesNotRepeatConsumerCompletion(String failurePhase) {
		final boolean consumerFails = !failurePhase.equals( "post action" );
		final boolean completed = !failurePhase.equals( "unreleased consumer" );
		final var original = new IllegalStateException( failurePhase );
		final var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		final var context = mock( ExecutionContext.class );
		when( context.getSession() ).thenReturn( session );
		when( context.getQueryOptions() ).thenReturn( mock( QueryOptions.class ) );
		final var select = mock( JdbcSelect.class );
		final var runner = mock( JdbcActionExecution.class );
		when( select.createActionExecution() ).thenReturn( runner );
		doThrow( original ).when( runner ).performPostActions( eq( true ), any(), any(), any(), isNull() );
		final var values = mock( JdbcValues.class );
		final RowReader<Object[]> reader = mock( RowReader.class );
		final ResultsConsumer<Object, Object[]> consumer = new ResultsConsumer<>() {
			@Override public boolean canResultsBeCached() { return false; }
			@Override public Object consume(JdbcValues jdbcValues, SharedSessionContractImplementor s,
					JdbcValuesSourceProcessingOptions options,
					JdbcValuesSourceProcessingState state,
					RowProcessingState rowState, RowReader<Object[]> rowReader) {
				// Represents a consumer's completion, including cache finalization.
				if ( completed ) { jdbcValues.finishUp( s ); }
				if ( consumerFails ) { throw original; }
				return new Object();
			}
		};
		final var executor = new JdbcSelectExecutorStandardImpl() {
			@Override
			protected JdbcValues resolveJdbcValuesSource(String identifier, JdbcSelect jdbcSelect,
					boolean cacheable, ExecutionContext executionContext, ResultSetAccess access) {
				return values;
			}
		};
		try ( var accesses = mockConstruction( DeferredResultSetAccess.class );
				var helpers = mockStatic( ResultsHelper.class ) ) {
			helpers.when( () -> ResultsHelper.createRowReader( any(), any(), any(), same( values ) ) ).thenReturn( reader );
			assertThatThrownBy( () -> executor.executeQuery( select, null, context,
					(RowTransformer<Object[]>) row -> row, Object[].class, null, consumer ) ).isSameAs( original );
			verify( values, times( completed ? 1 : 0 ) ).finishUp( session );
			verify( accesses.constructed().get( 0 ) ).release();
		}
	}

	private static void executeFailingSetup(Throwable original, JdbcActionExecution runner) {
		final var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		final var context = mock( ExecutionContext.class );
		when( context.getSession() ).thenReturn( session );
		when( context.getQueryOptions() ).thenReturn( mock( QueryOptions.class ) );
		final var select = mock( JdbcSelect.class );
		when( select.createActionExecution() ).thenReturn( runner );
		final var executor = new JdbcSelectExecutorStandardImpl() {
			@Override
			protected JdbcValues resolveJdbcValuesSource(String identifier, JdbcSelect jdbcSelect,
					boolean cacheable, ExecutionContext executionContext, ResultSetAccess access) {
				if ( original instanceof Error error ) {
					throw error;
				}
				throw (RuntimeException) original;
			}
		};
		assertThatThrownBy( () -> executor.executeQuery( select, null, context, null, Object[].class,
				null, mock( ResultsConsumer.class ) ) ).isSameAs( original );
	}

	private static Throwable failure(boolean error, String message) {
		return error ? new AssertionError( message ) : new IllegalStateException( message );
	}
}
