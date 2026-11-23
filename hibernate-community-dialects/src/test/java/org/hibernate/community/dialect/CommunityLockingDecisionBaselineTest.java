package org.hibernate.community.dialect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.IllegalQueryOperationException;
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

import org.hibernate.testing.orm.junit.FailureExpected;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
/// Tests marked `@FailureExpected` fail on the current code for the stated
/// finding. The build runs them with failure validation enabled, so each one
/// is verified to still fail and the build breaks as soon as one starts to
/// pass. Remove each marker when the corresponding change lands.
class CommunityLockingDecisionBaselineTest {
	private static final NavigablePath WORK_ITEM = new NavigablePath( "WorkItem" );

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Oracle legacy: set operation

	@Test
	@FailureExpected(reason = "Finding 6: the follow-on branch for set operations is inverted, so ALLOW yields NONE")
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
	@FailureExpected(reason = "Finding 6: the follow-on branch for set operations is inverted, so IGNORE yields FOLLOW_ON")
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
	@FailureExpected(reason = "Finding 6: the follow-on branch for set operations is inverted, so ALLOW yields NONE")
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
	@FailureExpected(reason = "Finding 6: the follow-on branch for set operations is inverted, so IGNORE yields FOLLOW_ON")
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
	@FailureExpected(reason = "Finding 2: the follow-on decision is made twice, from two signals; Teradata renders its prefix, reports RENDERED and also gets a follow-on action")
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
}
