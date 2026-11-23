package org.hibernate.orm.test.dialect.unit.locking;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.lock.internal.LockingSqlRewriterSupport;
import org.hibernate.dialect.lock.internal.TransactSQLLockingSupport;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteRequest;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteResult;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Exercises ASE skip-locked rendering across SQL AST and completed-SQL paths.
///
/// @implNote No need to run this for every CI db job; limit to the sybase run.
///
/// @author Steve Ebersole
@BaseUnitTest
@RequiresDialect(SybaseASEDialect.class)
class SybaseSkipLockedRenderingTest {
	private final SybaseASEDialect dialect = new SybaseASEDialect();

	@ParameterizedTest
	@EnumSource(value = LockMode.class, names = { "PESSIMISTIC_READ", "PESSIMISTIC_WRITE", "UPGRADE_SKIPLOCKED" })
	void skipLockedRetainsQueryLevelProtection(LockMode mode) {
		var options = new LockOptions( mode );
		if ( mode != LockMode.UPGRADE_SKIPLOCKED ) {
			options.setTimeout( Timeouts.SKIP_LOCKED );
		}
		String clauseSql = mode == LockMode.PESSIMISTIC_READ ? " at isolation 2" : " for update";
		assertThat( new SimpleSelect( dialect )
				.setTableName( "work_item" ).addColumn( "id" ).setLockOptions( options ).toStatementString() )
				.isEqualTo( "select id from work_item readpast" + clauseSql );
		var clause = new StringBuilder();
		dialect.getLockingClauseStrategy( new QuerySpec( true ), options ).render( clause::append );
		assertThat( clause ).hasToString( clauseSql );
	}

	@ParameterizedTest
	@EnumSource(value = PessimisticLockKind.class, names = { "SHARE", "UPDATE" })
	void nativeSqlIncludesHintsAndRetainedProtection(PessimisticLockKind kind) {
		var result = dialect.getLockingSupport().getLockingSqlRewriter().rewrite( new LockingSqlRewriteRequest(
				"select w.id from work_item w where w.id > 0 order by w.id", kind, Timeouts.SKIP_LOCKED,
				List.of( new LockingClauseRequest.TableTarget( "w" ) ) ) );
		assertThat( result.outcome() ).isEqualTo( LockingSqlRewriteResult.Outcome.APPLIED );
		assertThat( result.sql() ).isEqualTo(
				"select w.id from work_item w readpast where w.id > 0 order by w.id"
						+ (kind == PessimisticLockKind.SHARE ? " at isolation 2" : " for update") );
	}

	@Test
	void disabledServerOptionRejectsExclusiveRendering() {
		var support = TransactSQLLockingSupport.forSybaseASE( false );
		assertThat( support.getTransactionConcurrencyResolver() )
				.isSameAs( TransactSQLLockingSupport.SYBASE_ASE.getTransactionConcurrencyResolver() );
		assertThatThrownBy( () -> support.getLockingClauseRenderer().render(
				new LockingClauseRequest( PessimisticLockKind.UPDATE, Timeouts.SKIP_LOCKED, List.of() ) ) )
				.isInstanceOf( org.hibernate.query.IllegalQueryOperationException.class )
				.hasMessageContaining( "requires the server option 'select for update'" );
		assertThatThrownBy( () -> support.getLockingSqlRewriter().rewrite( new LockingSqlRewriteRequest(
				"select w.id from work_item w", PessimisticLockKind.UPDATE, Timeouts.SKIP_LOCKED,
				List.of( new LockingClauseRequest.TableTarget( "w" ) ) ) ) )
				.isInstanceOf( org.hibernate.query.IllegalQueryOperationException.class );
		assertThat( support.getLockingClauseRenderer().render(
				new LockingClauseRequest( PessimisticLockKind.SHARE, Timeouts.SKIP_LOCKED, List.of() ) ) )
				.isEqualTo( " at isolation 2" );
	}

	@Test
	void missingNativeTargetsAreStillUnsupported() {
		assertThat( LockingSqlRewriterSupport.rewrite( dialect.getLockingSupport(),
				"select id from work_item", new LockOptions( LockMode.PESSIMISTIC_WRITE ).setTimeout( Timeouts.SKIP_LOCKED ),
				null ).outcome() ).isEqualTo( LockingSqlRewriteResult.Outcome.UNSUPPORTED );
	}

	@Test
	void ordinaryLockingStillUsesHoldlock() {
		assertThat( new SimpleSelect( dialect ).setTableName( "work_item" ).addColumn( "id" )
				.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) ).toStatementString() )
				.isEqualTo( "select id from work_item holdlock" );
	}

	@Test
	void unlockedSqlHasNoIsolationClause() {
		assertThat( new SimpleSelect( dialect ).setTableName( "work_item" ).addColumn( "id" ).toStatementString() )
				.isEqualTo( "select id from work_item" );
	}
}
