package org.hibernate.orm.test.sql.exec.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.H2LockingSupport;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.FollowOnLockingPolicy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingSqlRewriter;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitOffsetLimitHandler;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.internal.lock.LoadedValuesCollectorFactory;
import org.hibernate.sql.exec.internal.lock.LockingHelper;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcActionExecution;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.exec.spi.JdbcPaginationApplication;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.exec.spi.PostAction;
import org.hibernate.sql.exec.spi.PreAction;
import org.hibernate.sql.exec.spi.StatementAccess;
import org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess;
import org.hibernate.sql.results.jdbc.internal.JdbcSelectSqlFinalizer;
import org.hibernate.sql.results.jdbc.internal.ResultSetAccess;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.spi.ResultsConsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies rejection before execution resources and action materialization,
/// and invocation-local reuse of successful raw-SQL finalization.
///
/// @author Steve Ebersole
class SkipLockedPreflightTest {
	@ParameterizedTest
	@EnumSource(value = JdbcLockingApplication.class, names = { "FOLLOW_ON", "RAW_SQL" })
	void rejectionPrecedesConnectionAndActionFactory(JdbcLockingApplication application) {
		final var fixture = new Fixture( application, true );
		assertThatThrownBy( () -> fixture.execute( JdbcSelectExecutorStandardImpl.INSTANCE ) )
				.isInstanceOf( IllegalQueryOperationException.class )
				.hasMessage( "SKIP_LOCKED is not supported with follow-on locking; use direct locking for a supported query shape" );
		verify( fixture.session, never() ).getJdbcCoordinator();
		verify( fixture.select, never() ).createActionExecution();
		assertThat( fixture.events ).doesNotContain( "factory", "pre", "post", "cleanup" );
	}

	@Test
	void validRawSqlIsFinalizedOnceAndReusedAfterPreActions() {
		final var fixture = new Fixture( JdbcLockingApplication.RAW_SQL, false );
		fixture.options.setComment( "preflight" );
		fixture.options.addDatabaseHint( "test hint" );
		final var setupFailure = new IllegalStateException( "after deferred access" );
		final var executor = new JdbcSelectExecutorStandardImpl() {
			@Override
			protected JdbcValues resolveJdbcValuesSource(String identifier, JdbcSelect select, boolean cacheable,
					ExecutionContext context, ResultSetAccess access) {
				fixture.events.add( "deferred" );
				final var deferred = (DeferredResultSetAccess) access;
				assertThat( deferred.getFinalSql() ).isEqualTo(
						"/* preflight */ select id from work_item limit ? offset ? for update skip locked /* hint */" );
				assertThat( deferred.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
				assertThat( deferred.getLimit() ).isSameAs( fixture.options.getLimit() );
				throw setupFailure;
			}
		};
		for ( int invocation = 0; invocation < 2; invocation++ ) {
			assertThatThrownBy( () -> fixture.execute( executor ) ).isSameAs( setupFailure );
		}
		assertThat( fixture.events ).containsExactly(
				"pagination", "rewrite", "policy", "hint", "factory", "pre", "deferred", "cleanup",
				"pagination", "rewrite", "policy", "hint", "factory", "pre", "deferred", "cleanup" );
	}

	@Test
	void prefinalizedConstructorPreservesPaginationBindings() throws java.sql.SQLException {
		final var fixture = new Fixture( JdbcLockingApplication.RAW_SQL, false );
		final var result = JdbcSelectSqlFinalizer.finalizeSql(
				fixture.select, JdbcParameterBindings.NO_BINDINGS, fixture.context );
		final var access = new DeferredResultSetAccess( fixture.select, JdbcParameterBindings.NO_BINDINGS,
				fixture.context, null, 0, result ) {
			void bind(java.sql.PreparedStatement statement) throws java.sql.SQLException {
				bindParameters( statement );
			}
		};
		final var statement = mock( java.sql.PreparedStatement.class );
		access.bind( statement );
		verify( statement ).setInt( 1, 10 );
		verify( statement ).setInt( 2, 5 );
		assertThat( fixture.events ).containsExactly( "pagination", "rewrite", "policy" );
		verify( fixture.session, never() ).getJdbcCoordinator();
	}

	@ParameterizedTest
	@EnumSource(value = JdbcLockingApplication.class, names = { "NONE", "RENDERED", "RAW_SQL" })
	void otherRequestsRetainFinalizationAfterPreActions(JdbcLockingApplication application) {
		final var fixture = new Fixture( application, false );
		if ( application == JdbcLockingApplication.RAW_SQL ) {
			fixture.options.getLockOptions().setTimeout( Timeouts.WAIT_FOREVER );
		}
		final var failure = new IllegalStateException( "after deferred access" );
		final var executor = new JdbcSelectExecutorStandardImpl() {
			@Override
			protected JdbcValues resolveJdbcValuesSource(String identifier, JdbcSelect select, boolean cacheable,
					ExecutionContext context, ResultSetAccess access) {
				throw failure;
			}
		};
		assertThatThrownBy( () -> fixture.execute( executor ) ).isSameAs( failure );
		assertThat( fixture.events ).startsWith( "factory", "pre", "pagination" ).endsWith( "cleanup" );
	}

	@Test
	void builderRejectsBeforeAddingSecondaryActions() {
		final var builder = new JdbcSelectWithActions.Builder();
		builder.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setTimeout( Timeouts.SKIP_LOCKED ) );
		builder.setIsFollowOnLockStrategy( true );
		assertThatThrownBy( builder::build ).isInstanceOf( IllegalQueryOperationException.class );
	}

	@Test
	void skipIntentIncludesLegacyModeAndRequiresPessimisticLocking() {
		assertThat( LockingHelper.isSkipLockedRequest( null ) ).isFalse();
		assertThat( LockingHelper.isSkipLockedRequest( new LockOptions( LockMode.OPTIMISTIC )
				.setTimeout( Timeouts.SKIP_LOCKED ) ) ).isFalse();
		assertThat( LockingHelper.isSkipLockedRequest( new LockOptions( LockMode.PESSIMISTIC_WRITE ) ) ).isFalse();
		final var legacy = new LockOptions( LockMode.UPGRADE_SKIPLOCKED );
		assertThat( legacy.getTimeout() ).isEqualTo( Timeouts.WAIT_FOREVER );
		assertThat( LockingHelper.isSkipLockedRequest( legacy ) ).isTrue();
		assertThatThrownBy( () -> LockingHelper.validateFollowOnLocking( legacy ) )
				.isInstanceOf( IllegalQueryOperationException.class );
		assertThat( legacy.getTimeout() ).isEqualTo( Timeouts.WAIT_FOREVER );
	}

	@Test
	void nullOptionsRetainOriginalSqlAndJdbcPaginationWithoutServices() {
		final var context = mock( ExecutionContext.class );
		final var select = mock( JdbcSelect.class );
		when( select.getSqlString() ).thenReturn( "select id from work_item" );
		when( select.getRowsToSkip() ).thenReturn( 3 );
		when( select.getMaxRows() ).thenReturn( 9 );
		final var result = JdbcSelectSqlFinalizer.finalizeSql( select, null, context );
		assertThat( result.sql() ).isEqualTo( "select id from work_item" );
		assertThat( result.lockingApplication() ).isEqualTo( JdbcLockingApplication.NONE );
		assertThat( result.paginationInstructions().rowsToSkip() ).isEqualTo( 3 );
		assertThat( result.paginationInstructions().maxRows() ).isEqualTo( 9 );
		verify( context, never() ).getSession();
	}

	private static class Fixture {
		final List<String> events = new ArrayList<>();
		final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		final ExecutionContext context = mock( ExecutionContext.class );
		final JdbcSelect select = mock( JdbcSelect.class );
		final QueryOptionsImpl options = new QueryOptionsImpl();

		Fixture(JdbcLockingApplication application, boolean followOn) {
			options.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE ).setTimeout( Timeouts.SKIP_LOCKED )
					.setFollowOnStrategy( Locking.FollowOn.ALLOW );
			options.getLimit().setFirstRow( 5 );
			options.getLimit().setMaxRows( 10 );
			when( context.getSession() ).thenReturn( session );
			when( context.getQueryOptions() ).thenReturn( options );
			when( session.getFactory().getSessionFactoryOptions().isCommentsEnabled() ).thenReturn( true );
			when( select.getSqlString() ).thenReturn( "select id from work_item" );
			when( select.getLockingApplication() ).thenReturn( application );
			when( select.getPaginationApplication() ).thenReturn( JdbcPaginationApplication.RAW_SQL );
			when( select.getMaxRows() ).thenReturn( Integer.MAX_VALUE );
			when( select.getLoadedValuesCollectorFactory() ).thenReturn( mock( LoadedValuesCollectorFactory.class ) );
			final var support = new LockingSupportSimple(
					PessimisticLockStyle.CLAUSE, LockTimeoutType.QUERY,
					OuterJoinLockingType.FULL, ConnectionLockTimeoutStrategy.NONE ) {
				@Override public LockingClauseRenderer getLockingClauseRenderer() {
					return H2LockingSupport.INSTANCE.getLockingClauseRenderer();
				}
				@Override public LockingSqlRewriter getLockingSqlRewriter() {
					final var delegate = super.getLockingSqlRewriter();
					return request -> { events.add( "rewrite" ); return delegate.rewrite( request ); };
				}
				@Override public FollowOnLockingPolicy getFollowOnLockingPolicy() {
					return request -> {
						events.add( "policy" );
						assertThat( request.sql() ).endsWith( "limit ? offset ?" );
						return followOn;
					};
				}
			};
			final var dialect = new H2Dialect() {
				@Override public LockingSupport getLockingSupport() { return support; }
				@Override public LimitHandler getLimitHandler() {
					return request -> { events.add( "pagination" ); return LimitOffsetLimitHandler.INSTANCE.processSql( request ); };
				}
				@Override public String getQueryHintString(String sql, List<String> hints) {
					events.add( "hint" );
					return sql + " /* hint */";
				}
			};
			when( session.getJdbcServices().getDialect() ).thenReturn( dialect );
			when( session.getJdbcServices().getParameterMarkerStrategy() )
					.thenReturn( ParameterMarkerStrategyStandard.INSTANCE );
			when( select.createActionExecution() ).thenAnswer( invocation -> {
				events.add( "factory" );
				return JdbcActionExecution.fromActions(
						new PreAction[] { (a, c, e) -> events.add( "pre" ) },
						new PostAction[] { new PostAction() {
							@Override public boolean shouldRunAfterFail() { return true; }
							@Override public void performPostAction(StatementAccess a, java.sql.Connection c,
									ExecutionContext e, LoadedValuesCollector l) { events.add( "cleanup" ); }
						} } );
			} );
		}

		void execute(JdbcSelectExecutorStandardImpl executor) {
			executor.executeQuery( select, JdbcParameterBindings.NO_BINDINGS, context, null,
					Object[].class, null, mock( ResultsConsumer.class ) );
		}
	}
}
