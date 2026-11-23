package org.hibernate.community.dialect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
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
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
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
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Community-dialect counterpart of the core `LockingDecisionBaselineTest`,
/// for the locking findings in
/// `design/working/sql-ast-translator-convention-proposal.adoc`.
///
/// Each test states the behavior the working specification calls for. Nothing
/// here touches a database, so these assert rendered SQL, the reported locking
/// application and whether a follow-on action was attached, not that rows are
/// locked.
///
/// These were written against the code as it was before the locking changes,
/// where the tests tied to a finding failed and were marked as expected
/// failures. The markers came off as the changes landed.
class CommunityLockingDecisionBaselineTest {
	private static final NavigablePath WORK_ITEM = new NavigablePath( "WorkItem" );

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Oracle legacy: set operation

	@Test
	void oracleLegacySetOperationWithAllowAttachesFollowOnLocking() {
		oracleLegacySetOperationAttachesFollowOnLocking( Locking.FollowOn.ALLOW );
	}

	@Test
	void oracleLegacySetOperationWithForceAttachesFollowOnLocking() {
		oracleLegacySetOperationAttachesFollowOnLocking( Locking.FollowOn.FORCE );
	}

	private void oracleLegacySetOperationAttachesFollowOnLocking(Locking.FollowOn followOn) {
		final var fixture = new Fixture( new OracleLegacyDialect( DatabaseVersion.make( 12 ) ) );
		final var translator = new RecordingOracleLegacy<>( fixture.request( fixture.lockedUnion() ) );

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
	void oracleLegacySetOperationWithIgnoreReportsNoLocking() {
		final var fixture = new Fixture( new OracleLegacyDialect( DatabaseVersion.make( 12 ) ) );
		final var translator = new RecordingOracleLegacy<>( fixture.request( fixture.lockedUnion() ) );

		final JdbcSelect operation = translator.translate( null, options( Locking.FollowOn.IGNORE ) );

		assertThat( operation.getSqlString() ).doesNotContain( "for update" );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
		assertThat( operation.getLockingApplication() )
				.as( "calls: %s", translator.calls )
				.isEqualTo( JdbcLockingApplication.NONE );
	}

	@Test
	void oracleLegacySetOperationWithDisallowIsRejected() {
		final var fixture = new Fixture( new OracleLegacyDialect( DatabaseVersion.make( 12 ) ) );
		final var translator = new RecordingOracleLegacy<>( fixture.request( fixture.lockedUnion() ) );

		assertThatThrownBy( () -> translator.translate( null, options( Locking.FollowOn.DISALLOW ) ) )
				.isInstanceOf( IllegalQueryOperationException.class );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TimesTen: set operation, including its inverted follow-on branch

	@Test
	void timesTenSetOperationWithAllowAttachesFollowOnLocking() {
		timesTenSetOperationAttachesFollowOnLocking( Locking.FollowOn.ALLOW );
	}

	@Test
	void timesTenSetOperationWithForceAttachesFollowOnLocking() {
		timesTenSetOperationAttachesFollowOnLocking( Locking.FollowOn.FORCE );
	}

	private void timesTenSetOperationAttachesFollowOnLocking(Locking.FollowOn followOn) {
		final var fixture = new Fixture( new TimesTenDialect() );
		final var translator = new RecordingTimesTen<>( fixture.request( fixture.lockedUnion() ) );

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
	void timesTenSetOperationWithIgnoreReportsNoLocking() {
		final var fixture = new Fixture( new TimesTenDialect() );
		final var translator = new RecordingTimesTen<>( fixture.request( fixture.lockedUnion() ) );

		final JdbcSelect operation = translator.translate( null, options( Locking.FollowOn.IGNORE ) );

		assertThat( operation.getSqlString() ).doesNotContain( "for update" );
		assertThat( operation.getLoadedValuesCollectorFactory() )
				.as( "calls: %s", translator.calls )
				.isNull();
		assertThat( operation.getLockingApplication() )
				.as( "calls: %s", translator.calls )
				.isEqualTo( JdbcLockingApplication.NONE );
	}

	@Test
	void timesTenSetOperationWithDisallowIsRejected() {
		final var fixture = new Fixture( new TimesTenDialect() );
		final var translator = new RecordingTimesTen<>( fixture.request( fixture.lockedUnion() ) );

		assertThatThrownBy( () -> translator.translate( null, options( Locking.FollowOn.DISALLOW ) ) )
				.isInstanceOf( IllegalQueryOperationException.class );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Teradata: locking prefix against the follow-on decision

	@Test
	void teradataRendersItsLockingPrefixWhenLockingInSql() {
		final var fixture = new Fixture( new TeradataDialect( DatabaseVersion.make( 14 ) ) );

		final JdbcSelect operation = new TeradataSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( fixture.lockedRoot() ) ) )
				.translate( null, options( Locking.FollowOn.ALLOW ) );

		assertThat( operation.getSqlString().trim() ).startsWithIgnoringCase( "locking" );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
	}

	@Test
	void teradataDoesNotRenderItsLockingPrefixWhenFollowOnIsUsed() {
		final var fixture = new Fixture( new TeradataDialect( DatabaseVersion.make( 14 ) ) );
		final var querySpec = fixture.lockedRoot();
		// DISTINCT cannot be combined with a locking clause, so follow-on locking is chosen
		querySpec.getSelectClause().makeDistinct( true );

		final JdbcSelect operation = new TeradataSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( querySpec ) ) )
				.translate( null, options( Locking.FollowOn.ALLOW ) );

		assertThat( operation.getLoadedValuesCollectorFactory() ).as( "follow-on action attached" ).isNotNull();
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.FOLLOW_ON );
		assertThat( operation.getSqlString() ).doesNotContainIgnoringCase( "locking" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Oracle legacy locking wrapper: the wrapper is the locking target

	@ParameterizedTest
	@EnumSource(value = Locking.FollowOn.class, names = { "ALLOW", "DISALLOW", "IGNORE" })
	void oracleLegacyLockingWrapperRendersTheLockingClauseOnce(Locking.FollowOn followOn) {
		final var fixture = new Fixture( new OracleLegacyDialect( DatabaseVersion.make( 12 ) ) );
		final var querySpec = fixture.lockedEntityRoot();
		querySpec.setFetchClauseExpression( new QueryLiteral<>( 5, fixture.integerType ), FetchClauseType.ROWS_ONLY );
		final var translator = new RecordingOracleLegacy<>( fixture.request( new SelectStatement( querySpec ) ) );

		final JdbcSelect operation = translator.translate( null, options( followOn ) );

		// the SQL is what this translator rendered before the wrapper became the locking target
		assertThat( operation.getSqlString() ).isEqualTo(
				"select 1 from work_item w where w.id in (select r_0_.c0 from"
						+ " (select w.id c0,rownum rn from work_item w) r_0_ where r_0_.rn<=5) for update" );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
		assertThat( translator.calls ).as( "calls: %s", translator.calls ).hasSize( 1 );
		assertThat( translator.calls.get( 0 ).queryPartStackDepth() ).as( "decided within the wrapper" ).isOne();
	}

	/// Also the window-paginated case: the query is rendered through the
	/// row-numbering emulation and the decision is still made once.
	@Test
	void oracleLegacyForcedFollowOnUsesNoLockingWrapper() {
		final var fixture = new Fixture( new OracleLegacyDialect( DatabaseVersion.make( 12 ) ) );
		final var querySpec = fixture.lockedEntityRoot();
		querySpec.setFetchClauseExpression( new QueryLiteral<>( 5, fixture.integerType ), FetchClauseType.ROWS_ONLY );
		final var translator = new RecordingOracleLegacy<>( fixture.request( new SelectStatement( querySpec ) ) );

		final JdbcSelect operation = translator.translate( null, options( Locking.FollowOn.FORCE ) );

		assertThat( operation.getSqlString() ).isEqualTo(
				"select * from (select 1 c0,rownum rn from work_item w) r_0_ where r_0_.rn<=5 order by r_0_.rn" );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.FOLLOW_ON );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNotNull();
		// the collector dereferences the paths while processing results
		operation.getLoadedValuesCollectorFactory().build().registerEntity( WORK_ITEM, null, null );
		assertThat( translator.calls ).as( "calls: %s", translator.calls ).hasSize( 1 );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Teradata: the decision is made once, though no trailing clause is rendered

	@ParameterizedTest
	@EnumSource(Locking.FollowOn.class)
	void teradataDecidesOnce(Locking.FollowOn followOn) {
		final var fixture = new Fixture( new TeradataDialect( DatabaseVersion.make( 14 ) ) );
		final var translator = new RecordingTeradata<>( fixture.request( new SelectStatement( fixture.lockedRoot() ) ) );

		translator.translate( null, options( followOn ) );

		assertThat( translator.calls ).as( "calls: %s", translator.calls ).hasSize( 1 );
		assertThat( translator.calls.get( 0 ).queryPartStackDepth() ).as( "decided within the query spec" ).isOne();
	}

	@Test
	void teradataWithoutLockingSupportRendersNoPrefix() {
		final var fixture = new Fixture( new TeradataDialect( DatabaseVersion.make( 13 ) ) );

		final JdbcSelect operation = new TeradataSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( fixture.lockedRoot() ) ) )
				.translate( null, options( Locking.FollowOn.ALLOW ) );

		assertThat( operation.getSqlString() ).doesNotContainIgnoringCase( "locking" );
		assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.NONE );
		assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Support

	private static QueryOptionsImpl options(Locking.FollowOn followOn) {
		final var options = new QueryOptionsImpl();
		options.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE ).setFollowOnStrategy( followOn );
		return options;
	}

	record Call(String querySpec, int queryPartStackDepth, String answer) {
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

		QuerySpec lockedRoot() {
			return querySpec( true, "work_item", "w", WORK_ITEM, WORK_ITEM );
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

		SelectStatement lockedUnion() {
			final var first = querySpec( false, "work_item", "w", WORK_ITEM, WORK_ITEM );
			final var second = querySpec( false, "archived_item", "a", null, new NavigablePath( "ArchivedItem" ) );
			return new SelectStatement( new QueryGroup( true, SetOperator.UNION_ALL, List.of( first, second ) ) );
		}
	}

	static final class RecordingOracleLegacy<T extends JdbcOperation> extends OracleLegacySqlAstTranslator<T> {
		final List<Call> calls = new ArrayList<>();

		RecordingOracleLegacy(SqlAstTranslationRequest<? extends Statement, T> request) {
			super( request );
		}

		@Override
		protected LockStrategy determineLockingStrategy(QuerySpec querySpec, Locking.FollowOn followOnStrategy) {
			final var answer = super.determineLockingStrategy( querySpec, followOnStrategy );
			calls.add( new Call(
					querySpec.getFromClause().getRoots().get( 0 ).getPrimaryTableReference().getIdentificationVariable(),
					getQueryPartStack().depth(), answer.name() ) );
			return answer;
		}
	}

	static final class RecordingTimesTen<T extends JdbcOperation> extends TimesTenSqlAstTranslator<T> {
		final List<Call> calls = new ArrayList<>();

		RecordingTimesTen(SqlAstTranslationRequest<? extends Statement, T> request) {
			super( request );
		}

		@Override
		protected LockStrategy determineLockingStrategy(QuerySpec querySpec, Locking.FollowOn followOnStrategy) {
			final var answer = super.determineLockingStrategy( querySpec, followOnStrategy );
			calls.add( new Call(
					querySpec.getFromClause().getRoots().get( 0 ).getPrimaryTableReference().getIdentificationVariable(),
					getQueryPartStack().depth(), answer.name() ) );
			return answer;
		}
	}

	static final class RecordingTeradata<T extends JdbcOperation> extends TeradataSqlAstTranslator<T> {
		final List<Call> calls = new ArrayList<>();

		RecordingTeradata(SqlAstTranslationRequest<? extends Statement, T> request) {
			super( request );
		}

		@Override
		protected LockStrategy determineLockingStrategy(QuerySpec querySpec, Locking.FollowOn followOnStrategy) {
			final var answer = super.determineLockingStrategy( querySpec, followOnStrategy );
			calls.add( new Call(
					querySpec.getFromClause().getRoots().get( 0 ).getPrimaryTableReference().getIdentificationVariable(),
					getQueryPartStack().depth(), answer.name() ) );
			return answer;
		}
	}
}
