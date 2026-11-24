package org.hibernate.orm.test.dialect.unit.locking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.Timeouts;
import org.hibernate.Locking;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.sql.ast.internal.OracleSqlAstTranslator;
import org.hibernate.dialect.sql.ast.internal.SQLServerSqlAstTranslator;
import org.hibernate.dialect.sql.ast.internal.SybaseASESqlAstTranslator;
import org.hibernate.dialect.sql.ast.internal.SybaseSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.PostgreSQLSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.common.FetchClauseType;
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
import org.hibernate.sql.ast.spi.query.predicate.ExistsPredicate;
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


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
/// These were written against the code as it was before the locking changes,
/// where the tests tied to a finding failed and were marked as expected
/// failures. The markers came off as the changes landed.
class LockingDecisionBaselineTest {
	private static final NavigablePath WORK_ITEM = new NavigablePath( "WorkItem" );
	private static final NavigablePath ARCHIVED_ITEM = new NavigablePath( "ArchivedItem" );
	private static final NavigablePath PURGED_ITEM = new NavigablePath( "PurgedItem" );

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
	void decisionIsMadeOncePerLockingTarget(Locking.FollowOn followOn) {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var translator = new RecordingPostgres<>( fixture.request( new SelectStatement( fixture.lockedRoot( true ) ) ) );

		translator.translate( null, options( followOn ) );

		assertThat( translator.calls ).as( "calls: %s", translator.calls ).hasSize( 1 );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Finding 3: a root's secondary tables follow a different rule

	@Test
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
	// Oracle locking wrapper: the wrapper is the locking target

	@ParameterizedTest
	@EnumSource(value = Locking.FollowOn.class, names = { "ALLOW", "DISALLOW", "IGNORE" })
	void oracleLockingWrapperRendersTheLockingClauseOnce(Locking.FollowOn followOn) {
		final var fixture = new Fixture( new OracleDialect() );
		final var querySpec = fixture.lockedEntityRoot();
		querySpec.setFetchClauseExpression( new QueryLiteral<>( 5, fixture.integerType ), FetchClauseType.ROWS_ONLY );
		final var translator = new RecordingOracle<>( fixture.request( new SelectStatement( querySpec ) ) );

		final JdbcSelect operation = translator.translate( null, options( followOn ) );

		assertThat( operation.getSqlString() ).isEqualTo(
				"select 1 from work_item w where w.id in (select w.id from work_item w fetch first 5 rows only) for update" );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
		assertThat( translator.calls ).as( "calls: %s", translator.calls ).hasSize( 1 );
		assertThat( translator.calls.get( 0 ).queryPartStackDepth() ).as( "decided within the wrapper" ).isOne();
	}

	@Test
	void oracleForcedFollowOnUsesNoLockingWrapper() {
		final var fixture = new Fixture( new OracleDialect() );
		final var querySpec = fixture.lockedEntityRoot();
		querySpec.setFetchClauseExpression( new QueryLiteral<>( 5, fixture.integerType ), FetchClauseType.ROWS_ONLY );
		final var translator = new RecordingOracle<>( fixture.request( new SelectStatement( querySpec ) ) );

		final JdbcSelect operation = translator.translate( null, options( Locking.FollowOn.FORCE ) );

		assertThat( operation.getSqlString() ).isEqualTo( "select 1 from work_item w fetch first 5 rows only" );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.FOLLOW_ON );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNotNull();
		assertThat( translator.calls ).as( "calls: %s", translator.calls ).hasSize( 1 );
		assertThat( translator.calls.get( 0 ).pathsToLock() ).containsExactly( WORK_ITEM );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// The decision is made once per locking target, whatever the query shape

	@ParameterizedTest
	@EnumSource(Locking.FollowOn.class)
	void decisionIsMadeOnceWithACte(Locking.FollowOn followOn) {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var statement = new SelectStatement( fixture.lockedRoot( true ) );
		statement.addCteStatement( fixture.cte() );
		final var translator = new RecordingPostgres<>( fixture.request( statement ) );

		translator.translate( null, options( followOn ) );

		assertThat( translator.calls ).as( "calls: %s", translator.calls ).hasSize( 1 );
		assertThat( translator.calls.get( 0 ).querySpec() ).as( "asked about the main query" ).isEqualTo( "w" );
		assertThat( translator.calls.get( 0 ).pathsToLock() ).containsExactly( WORK_ITEM );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Finding 9: a locking clause cannot be combined with a set operation

	@Test
	void setOperationUsesFollowOnLocking() {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var translator = new RecordingPostgres<>( fixture.request( fixture.lockedUnion() ) );

		final JdbcSelect operation = translator.translate( null, options( Locking.FollowOn.ALLOW ) );

		assertThat( operation.getSqlString() ).contains( "union all" ).doesNotContain( " for " );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.FOLLOW_ON );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNotNull();
		assertThat( translator.calls ).as( "calls: %s", translator.calls ).hasSize( 1 );
	}

	@Test
	void setOperationWithIgnoreAppliesNoLocking() {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var translator = new RecordingPostgres<>( fixture.request( fixture.lockedUnion() ) );

		final JdbcSelect operation = translator.translate( null, options( Locking.FollowOn.IGNORE ) );

		assertThat( operation.getSqlString() ).doesNotContain( " for " );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.NONE );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
	}

	@Test
	void setOperationWithDisallowIsRejected() {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var translator = new RecordingPostgres<>( fixture.request( fixture.lockedUnion() ) );

		assertThatThrownBy( () -> translator.translate( null, options( Locking.FollowOn.DISALLOW ) ) )
				.isInstanceOf( IllegalQueryOperationException.class )
				.hasMessageContaining( "set operators" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQL Server locks through table hints, which every branch of a set operation carries

	@ParameterizedTest
	@EnumSource(Locking.FollowOn.class)
	void sqlServerLocksEveryBranchOfASetOperation(Locking.FollowOn followOn) {
		final var fixture = new Fixture( new SQLServerDialect() );

		final JdbcSelect operation = new SQLServerSqlAstTranslator<JdbcSelect>(
				fixture.request( fixture.lockedUnionOfEntities() ) ).translate( null, options( followOn ) );

		assertThat( operation.getSqlString() ).isEqualTo(
				"select 1 from work_item w with (updlock,holdlock,rowlock)"
						+ " union all select 1 from archived_item a with (updlock,holdlock,rowlock)" );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
	}

	@Test
	void sqlServerLocksEveryBranchOfANestedSetOperation() {
		final var fixture = new Fixture( new SQLServerDialect() );
		final var nested = new QueryGroup( false, SetOperator.UNION_ALL, List.of(
				fixture.querySpec( false, "archived_item", "a", ARCHIVED_ITEM, ARCHIVED_ITEM ),
				fixture.querySpec( false, "purged_item", "p", PURGED_ITEM, PURGED_ITEM ) ) );
		final var statement = new SelectStatement( new QueryGroup( true, SetOperator.UNION_ALL, List.of(
				fixture.querySpec( false, "work_item", "w", WORK_ITEM, WORK_ITEM ), nested ) ) );

		final JdbcSelect operation = new SQLServerSqlAstTranslator<JdbcSelect>( fixture.request( statement ) )
				.translate( null, options( Locking.FollowOn.ALLOW ) );

		assertThat( operation.getSqlString() )
				.contains( "work_item w with (updlock,holdlock,rowlock)" )
				.contains( "archived_item a with (updlock,holdlock,rowlock)" )
				.contains( "purged_item p with (updlock,holdlock,rowlock)" );
	}

	@Test
	void sqlServerDoesNotLockASetOperationInASubquery() {
		final var fixture = new Fixture( new SQLServerDialect() );
		final var root = fixture.lockedRoot( true );
		final var subquery = new QueryGroup( false, SetOperator.UNION_ALL, List.of(
				fixture.querySpec( false, "archived_item", "a", null, ARCHIVED_ITEM ),
				fixture.querySpec( false, "purged_item", "p", null, PURGED_ITEM ) ) );
		root.applyPredicate( new ExistsPredicate( new SelectStatement( subquery ), false, null ) );

		final JdbcSelect operation = new SQLServerSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( root ) ) ).translate( null, options( Locking.FollowOn.ALLOW ) );

		assertThat( operation.getSqlString() ).isEqualTo(
				"select 1 from work_item w with (updlock,holdlock,rowlock) where exists("
						+ "select 1 from archived_item a union all select 1 from purged_item p)" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Sybase locks through table hints, which every branch of a set operation carries

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			PESSIMISTIC_READ  | select 1 from work_item w holdlock union all select 1 from archived_item a holdlock
			PESSIMISTIC_WRITE | select 1 from work_item w holdlock union all select 1 from archived_item a holdlock
			""")
	void sybaseAseSetOperationLockHints(LockMode lockMode, String expected) {
		final var fixture = new Fixture( new SybaseASEDialect() );
		final var options = new QueryOptionsImpl();
		options.getLockOptions().setLockMode( lockMode );

		final JdbcSelect operation = new SybaseASESqlAstTranslator<JdbcSelect>(
				fixture.request( fixture.lockedUnionOfEntities() ) ).translate( null, options );

		assertThat( operation.getSqlString() ).isEqualTo( expected );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			PESSIMISTIC_READ  | select 1 from work_item w holdlock union all select 1 from archived_item a holdlock
			PESSIMISTIC_WRITE | select 1 from work_item w holdlock union all select 1 from archived_item a holdlock
			""")
	void sybaseSetOperationLockHints(LockMode lockMode, String expected) {
		final var fixture = new Fixture( new SybaseDialect() );
		final var options = new QueryOptionsImpl();
		options.getLockOptions().setLockMode( lockMode );

		final JdbcSelect operation = new SybaseSqlAstTranslator<JdbcSelect>(
				fixture.request( fixture.lockedUnionOfEntities() ) ).translate( null, options );

		assertThat( operation.getSqlString() ).isEqualTo( expected );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
	}

	@Test
	void sybaseAseDoesNotLockASetOperationInASubquery() {
		final var fixture = new Fixture( new SybaseASEDialect() );
		final var root = fixture.lockedRoot( true );
		final var subquery = new QueryGroup( false, SetOperator.UNION_ALL, List.of(
				fixture.querySpec( false, "archived_item", "a", null, ARCHIVED_ITEM ),
				fixture.querySpec( false, "purged_item", "p", null, PURGED_ITEM ) ) );
		root.applyPredicate( new ExistsPredicate( new SelectStatement( subquery ), false, null ) );

		final JdbcSelect operation = new SybaseASESqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( root ) ) ).translate( null, options( Locking.FollowOn.ALLOW ) );

		assertThat( operation.getSqlString() ).isEqualTo(
				"select 1 from work_item w holdlock where exists("
						+ "select 1 from archived_item a union all select 1 from purged_item p)" );
	}

	@Test
	void sybaseDoesNotLockASetOperationInASubquery() {
		final var fixture = new Fixture( new SybaseDialect() );
		final var root = fixture.lockedRoot( true );
		final var subquery = new QueryGroup( false, SetOperator.UNION_ALL, List.of(
				fixture.querySpec( false, "archived_item", "a", null, ARCHIVED_ITEM ),
				fixture.querySpec( false, "purged_item", "p", null, PURGED_ITEM ) ) );
		root.applyPredicate( new ExistsPredicate( new SelectStatement( subquery ), false, null ) );

		final JdbcSelect operation = new SybaseSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( root ) ) ).translate( null, options( Locking.FollowOn.ALLOW ) );

		assertThat( operation.getSqlString() ).isEqualTo(
				"select 1 from work_item w holdlock where exists("
						+ "select 1 from archived_item a union all select 1 from purged_item p)" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Sybase ASE locking set query: each arm becomes the locking target in turn

	@Test
	void sybaseAseLockingSetQueryRestoresItsTarget() {
		final var fixture = new Fixture( new SybaseASEDialect() );
		final var translator = new RecordingSybaseASE<>( fixture.request( fixture.lockedUnion() ) );
		final var options = new QueryOptionsImpl();
		options.getLockOptions().setLockMode( LockMode.PESSIMISTIC_READ ).setTimeout( Timeouts.SKIP_LOCKED );

		final JdbcSelect operation = translator.translate( null, options );

		// the arms carry no trailing locking clause of their own; one is rendered for the whole query
		assertThat( operation.getSqlString() ).isEqualTo(
				"select 1 from work_item w readpast union all select 1 from archived_item a at isolation 2" );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
		// one decision each time a query spec becomes the locking target: the two arms while
		// they are rendered, then the first arm again once it is restored as the target
		assertThat( translator.calls ).extracting( Call::querySpec ).containsExactly( "w", "a", "w" );
		assertThat( translator.calls ).extracting( Call::answer ).containsOnly( "CLAUSE" );
		assertThat( translator.calls.get( 2 ).queryPartStackDepth() ).as( "decided at finalization" ).isZero();
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

		/// A locked root whose model part is an entity with a single-column
		/// identifier, which is what the Oracle locking wrapper requires.
		QuerySpec lockedEntityRoot() {
			final var persister = mock( EntityPersister.class );
			when( persister.getQuerySpaces() ).thenReturn( new String[] { "work_item" } );
			final var identifier = mock( EntityIdentifierMapping.class );
			final var selectable = mock( SelectableMapping.class );
			when( selectable.getSelectionExpression() ).thenReturn( "id" );
			when( selectable.getSelectableName() ).thenReturn( "id" );
			when( selectable.getJdbcMapping() ).thenReturn( integerType );
			when( identifier.getJdbcTypeCount() ).thenReturn( 1 );
			when( identifier.forEachSelectable( anyInt(), any() ) ).thenAnswer( invocation -> {
				invocation.<SelectableConsumer>getArgument( 1 ).accept( 0, selectable );
				return 1;
			} );
			when( persister.getIdentifierMapping() ).thenReturn( identifier );
			final var querySpec = new QuerySpec( true );
			querySpec.applyRootPathForLocking( WORK_ITEM );
			querySpec.getFromClause().addRoot( new StandardTableGroup(
					true, WORK_ITEM, persister, "w", new NamedTableReference( "work_item", "w" ),
					false, new SqlAliasBaseConstant( "w" ), name -> false, null, factory ) );
			final EntityResult<?> result = mock( EntityResult.class );
			when( result.getNavigablePath() ).thenReturn( WORK_ITEM );
			when( result.getFetches() ).thenReturn( ImmutableFetchList.EMPTY );
			querySpec.applyDomainResultForLocking( result );
			querySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0, new QueryLiteral<>( 1, integerType ) ) );
			return querySpec;
		}

		/// A set operation whose branches each select an entity to lock, as an HQL
		/// set operation over entity queries does.
		SelectStatement lockedUnionOfEntities() {
			final var first = querySpec( false, "work_item", "w", WORK_ITEM, WORK_ITEM );
			final var second = querySpec( false, "archived_item", "a", ARCHIVED_ITEM, ARCHIVED_ITEM );
			return new SelectStatement( new QueryGroup( true, SetOperator.UNION_ALL, List.of( first, second ) ) );
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

	static final class RecordingSybaseASE<T extends JdbcOperation> extends SybaseASESqlAstTranslator<T> {
		final List<Call> calls = new ArrayList<>();

		RecordingSybaseASE(SqlAstTranslationRequest<? extends Statement, T> request) {
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
