package org.hibernate.orm.test.dialect.unit.locking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.sql.ast.internal.OracleSqlAstTranslator;
import org.hibernate.dialect.sql.ast.internal.SQLServerSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.PostgreSQLSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.cte.CteColumn;
import org.hibernate.sql.ast.spi.query.cte.CteStatement;
import org.hibernate.sql.ast.spi.query.cte.CteTable;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.junit.FailureExpected;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Baseline for the locking findings in
/// `design/working/sql-ast-translator-convention-proposal.adoc`.
///
/// Each test states the behavior the working specification calls for. They
/// build SQL ASTs by hand and translate them without a database or a
/// bootstrapped SessionFactory, so they assert rendered SQL, the reported
/// locking application and whether a follow-on action was attached. They
/// cannot assert that rows are actually locked; that needs a database-backed
/// test.
///
/// The recording translators capture, for each `determineLockingStrategy`
/// call, the query spec it was asked about, the depth of the query-part stack
/// at that moment, the answer, and the paths the locking strategy had
/// collected. A test that unexpectedly passes or fails should be read against
/// that record before a finding is revised.
///
/// Tests marked `@FailureExpected` fail on the current code for the stated
/// finding. The build runs them with failure validation enabled, so each one
/// is verified to still fail and the build breaks as soon as one starts to
/// pass. Remove each marker when the corresponding change lands.
class LockingDecisionBaselineTest {
	private static final NavigablePath WORK_ITEM = new NavigablePath( "WorkItem" );

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Finding 1: strategy built from the first query spec visited

	@Test
	void controlWithoutCteNamesTheLockTarget() {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var translator = new RecordingPostgres<>( fixture.request( new SelectStatement( fixture.lockedRoot( true ) ) ) );

		final JdbcSelect operation = translator.translate( null, options( Locking.FollowOn.ALLOW ) );

		assertThat( operation.getSqlString() ).containsPattern( "for (?:no key )?update of w$" );
		assertThat( translator.calls ).isNotEmpty();
		assertThat( translator.calls.get( 0 ).pathsToLock ).containsExactly( WORK_ITEM );
	}

	@Test
	@FailureExpected(reason = "Finding 1: the locking strategy is built from the CTE's query spec, so the clause names no lock target")
	void cteDoesNotChangeTheLockTarget() {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var statement = new SelectStatement( fixture.lockedRoot( true ) );
		statement.addCteStatement( fixture.cte() );
		final var translator = new RecordingPostgres<>( fixture.request( statement ) );

		final JdbcSelect operation = translator.translate( null, options( Locking.FollowOn.ALLOW ) );

		// confirm the CTE was rendered, not inlined
		assertThat( operation.getSqlString() ).startsWith( "with c" );
		assertThat( operation.getSqlString() ).containsPattern( "for (?:no key )?update of w$" );
		assertThat( translator.calls.get( 0 ).pathsToLock ).containsExactly( WORK_ITEM );
	}

	@Test
	@FailureExpected(reason = "Finding 1: the locking strategy is built from the CTE's query spec, so getPathsToLock() is null")
	void cteWithForcedFollowOnCollectsThePathsToLock() {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var statement = new SelectStatement( fixture.lockedRoot( true ) );
		statement.addCteStatement( fixture.cte() );
		final var translator = new RecordingPostgres<>( fixture.request( statement ) );

		final JdbcSelect operation = translator.translate( null, options( Locking.FollowOn.FORCE ) );

		assertThat( operation.getSqlString() ).startsWith( "with c" );
		assertThat( operation.getSqlString() ).doesNotContain( " for " );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.FOLLOW_ON );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNotNull();
		// the collector dereferences the paths while processing results
		operation.getLoadedValuesCollectorFactory().build().registerEntity( WORK_ITEM, null, null );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Finding 2: two signals for the follow-on decision

	@Test
	@FailureExpected(reason = "Finding 2: the follow-on decision is made twice, from two signals; the second call sees an empty query-part stack and no follow-on action is attached")
	void oracleSetOperationWithAllowAttachesFollowOnLocking() {
		oracleSetOperationAttachesFollowOnLocking( Locking.FollowOn.ALLOW );
	}

	@Test
	void oracleSetOperationWithForceAttachesFollowOnLocking() {
		oracleSetOperationAttachesFollowOnLocking( Locking.FollowOn.FORCE );
	}

	private void oracleSetOperationAttachesFollowOnLocking(Locking.FollowOn followOn) {
		final var fixture = new Fixture( new OracleDialect() );
		final var translator = new RecordingOracle<>( fixture.request( fixture.lockedUnion() ) );

		final JdbcSelect operation = translator.translate( null, options( followOn ) );

		assertThat( operation.getSqlString() ).contains( "union all" ).doesNotContain( "for update" );
		assertThat( operation.getLockingApplication() )
				.as( "calls: %s", translator.calls )
				.isEqualTo( JdbcLockingApplication.FOLLOW_ON );
		assertThat( operation.getLoadedValuesCollectorFactory() )
				.as( "follow-on action attached; calls: %s", translator.calls )
				.isNotNull();
	}

	@Test
	@FailureExpected(reason = "Finding 2: the follow-on decision is made twice, from two signals; a NONE decision is reported as RENDERED")
	void oracleSetOperationWithIgnoreReportsNoLocking() {
		final var fixture = new Fixture( new OracleDialect() );
		final var translator = new RecordingOracle<>( fixture.request( fixture.lockedUnion() ) );

		final JdbcSelect operation = translator.translate( null, options( Locking.FollowOn.IGNORE ) );

		assertThat( operation.getSqlString() ).doesNotContain( "for update" );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
		assertThat( operation.getLockingApplication() )
				.as( "calls: %s", translator.calls )
				.isEqualTo( JdbcLockingApplication.NONE );
	}

	@Test
	void oracleSetOperationWithDisallowIsRejected() {
		final var fixture = new Fixture( new OracleDialect() );
		final var translator = new RecordingOracle<>( fixture.request( fixture.lockedUnion() ) );

		assertThatThrownBy( () -> translator.translate( null, options( Locking.FollowOn.DISALLOW ) ) )
				.isInstanceOf( IllegalQueryOperationException.class );
	}

	@Test
	@FailureExpected(reason = "Finding 2: the follow-on decision is made twice, from two signals; a NONE decision is reported as RENDERED")
	void noneDecisionReportsNoLocking() {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var querySpec = fixture.lockedRoot( true );
		querySpec.getSelectClause().makeDistinct( true );
		final var translator = new RecordingPostgres<>( fixture.request( new SelectStatement( querySpec ) ) );

		final JdbcSelect operation = translator.translate( null, options( Locking.FollowOn.IGNORE ) );

		assertThat( operation.getSqlString() ).doesNotContain( " for " );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.NONE );
	}

	@ParameterizedTest
	@EnumSource(Locking.FollowOn.class)
	@FailureExpected(reason = "Finding 2: the follow-on decision is made twice, from two signals")
	void decisionIsMadeOncePerLockingTarget(Locking.FollowOn followOn) {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var translator = new RecordingPostgres<>( fixture.request( new SelectStatement( fixture.lockedRoot( true ) ) ) );

		translator.translate( null, options( followOn ) );

		assertThat( translator.calls ).as( "calls: %s", translator.calls ).hasSize( 1 );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Finding 3: a root's secondary tables follow a different rule

	@Test
	@FailureExpected(reason = "Finding 3: a root's secondary tables are hinted by a different rule from the root")
	void secondaryTableOfUnselectedRootIsNotHinted() {
		final var fixture = new Fixture( new SQLServerDialect() );
		final var querySpec = fixture.lockedRoot( false );
		fixture.addSecondaryTable( querySpec );

		final JdbcSelect operation = new SQLServerSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( querySpec ) ) )
				.translate( null, options( Locking.FollowOn.ALLOW ) );

		assertThat( operation.getSqlString() ).contains( "work_item_detail" ).doesNotContain( "updlock" );
	}

	@Test
	void secondaryTableOfSelectedRootIsHinted() {
		final var fixture = new Fixture( new SQLServerDialect() );
		final var querySpec = fixture.lockedRoot( true );
		fixture.addSecondaryTable( querySpec );

		final JdbcSelect operation = new SQLServerSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( querySpec ) ) )
				.translate( null, options( Locking.FollowOn.ALLOW ) );

		assertThat( operation.getSqlString() ).containsPattern( "work_item w with \\([^)]*updlock" );
		assertThat( operation.getSqlString() ).containsPattern( "work_item_detail d with \\([^)]*updlock" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Support

	private static QueryOptionsImpl options(Locking.FollowOn followOn) {
		final var options = new QueryOptionsImpl();
		options.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE ).setFollowOnStrategy( followOn );
		return options;
	}

	record Call(String querySpec, int queryPartStackDepth, String answer, Collection<NavigablePath> pathsToLock) {
	}

	static final class Fixture {
		final SessionFactoryImplementor factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		final TypeConfiguration types = new TypeConfiguration();
		final BasicType<Integer> integerType = types.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER );

		Fixture(Dialect dialect) {
			when( factory.getTypeConfiguration() ).thenReturn( types );
			when( factory.getJdbcServices().getDialect() ).thenReturn( dialect );
			when( factory.getJdbcSelectWithActionsBuilder() ).thenAnswer( invocation -> new JdbcSelectWithActions.Builder() );
		}

		SqlAstTranslationRequest.Select request(SelectStatement statement) {
			return new SqlAstTranslationRequest.Select( factory, statement );
		}

		QuerySpec lockedRoot(boolean selectedForLocking) {
			return querySpec( true, "work_item", "w", selectedForLocking ? WORK_ITEM : null, WORK_ITEM );
		}

		QuerySpec querySpec(boolean root, String table, String alias, NavigablePath lockRoot, NavigablePath path) {
			final var querySpec = new QuerySpec( root );
			if ( lockRoot != null ) {
				querySpec.applyRootPathForLocking( lockRoot );
			}
			// the constructor taking a join-name checker leaves the table-reference joins mutable
			querySpec.getFromClause().addRoot( new StandardTableGroup(
					true, path, mock( TableGroupProducer.class ), alias, new NamedTableReference( table, alias ),
					false, new SqlAliasBaseConstant( alias ), name -> false, null, factory ) );
			if ( lockRoot != null ) {
				// follow-on locking validates that the result shape covers every lock target
				final EntityResult<?> result = mock( EntityResult.class );
				when( result.getNavigablePath() ).thenReturn( lockRoot );
				when( result.getFetches() ).thenReturn( ImmutableFetchList.EMPTY );
				querySpec.applyDomainResultForLocking( result );
			}
			querySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0, new QueryLiteral<>( 1, integerType ) ) );
			return querySpec;
		}

		void addSecondaryTable(QuerySpec querySpec) {
			final var root = (StandardTableGroup) querySpec.getFromClause().getRoots().get( 0 );
			root.addTableReferenceJoin( new TableReferenceJoin(
					true, new NamedTableReference( "work_item_detail", "d" ), null ) );
		}

		CteStatement cte() {
			final var definition = querySpec( false, "other_item", "o", null, new NavigablePath( "OtherItem" ) );
			return new CteStatement(
					new CteTable( "c", List.of( new CteColumn( "x", integerType ) ) ),
					new SelectStatement( definition ) );
		}

		SelectStatement lockedUnion() {
			final var first = querySpec( false, "work_item", "w", WORK_ITEM, WORK_ITEM );
			final var second = querySpec( false, "archived_item", "a", null, new NavigablePath( "ArchivedItem" ) );
			return new SelectStatement( new QueryGroup( true, SetOperator.UNION_ALL, List.of( first, second ) ) );
		}
	}

	static final class RecordingPostgres<T extends JdbcOperation> extends PostgreSQLSqlAstTranslator<T> {
		final List<Call> calls = new ArrayList<>();

		RecordingPostgres(SqlAstTranslationRequest<? extends Statement, T> request) {
			super( request );
		}

		@Override
		protected LockStrategy determineLockingStrategy(QuerySpec querySpec, Locking.FollowOn followOnStrategy) {
			final var answer = super.determineLockingStrategy( querySpec, followOnStrategy );
			final var strategy = getLockingClauseStrategy();
			final var paths = strategy == null ? null : strategy.getPathsToLock();
			calls.add( new Call(
					querySpec.getFromClause().getRoots().get( 0 ).getPrimaryTableReference().getIdentificationVariable(),
					getQueryPartStack().depth(), answer.name(), paths == null ? null : List.copyOf( paths ) ) );
			return answer;
		}
	}

	static final class RecordingOracle<T extends JdbcOperation> extends OracleSqlAstTranslator<T> {
		final List<Call> calls = new ArrayList<>();

		RecordingOracle(SqlAstTranslationRequest<? extends Statement, T> request) {
			super( request );
		}

		@Override
		protected LockStrategy determineLockingStrategy(QuerySpec querySpec, Locking.FollowOn followOnStrategy) {
			final var answer = super.determineLockingStrategy( querySpec, followOnStrategy );
			final var strategy = getLockingClauseStrategy();
			final var paths = strategy == null ? null : strategy.getPathsToLock();
			calls.add( new Call(
					querySpec.getFromClause().getRoots().get( 0 ).getPrimaryTableReference().getIdentificationVariable(),
					getQueryPartStack().depth(), answer.name(), paths == null ? null : List.copyOf( paths ) ) );
			return answer;
		}
	}
}
