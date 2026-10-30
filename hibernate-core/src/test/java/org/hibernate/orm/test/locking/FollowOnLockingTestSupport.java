package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.util.Locale;

import org.hibernate.SharedSessionContract;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.BlockingDuration;
import org.hibernate.dialect.lock.spi.Operation;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.internal.SelectionQueryImpl;
import org.hibernate.query.sqm.sql.spi.SqmTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Test-local prerequisites and plan assertions for READ_COMMITTED follow-on
/// acquisition. An unexpected strategy or failure is never an eligibility probe.
///
/// @author Steve Ebersole
public final class FollowOnLockingTestSupport {
	private FollowOnLockingTestSupport() {
	}

	/// Evaluated before fixture bootstrap so unsupported isolation settings and
	/// documented direct-locking overrides do not initialize these factories.
	public static class Eligibility implements ExecutionCondition {
		@Override
		public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
			final var dialectClass = DialectContext.getDialectClass();
			if ( SQLServerDialect.class.isAssignableFrom( dialectClass ) || SybaseDialect.class.isAssignableFrom( dialectClass ) ) {
				return ConditionEvaluationResult.disabled( "Dialect deliberately uses direct locking even with FORCE" );
			}
			if ( SpannerDialect.class.isAssignableFrom( dialectClass ) || SpannerPostgreSQLDialect.class.isAssignableFrom( dialectClass ) ) {
				return ConditionEvaluationResult.disabled( "Database profile does not support READ_COMMITTED" );
			}
			final var dialect = DialectContext.getDialect();
			if ( dialect instanceof CockroachDialect && dialect.getVersion().isBefore( 23, 2 ) ) {
				return ConditionEvaluationResult.disabled( "Database profile does not support READ_COMMITTED" );
			}
			if ( dialect.getLockingSupport().getMetadata().getPessimisticLockStyle() == PessimisticLockStyle.NONE ) {
				return ConditionEvaluationResult.disabled( "Dialect does not support pessimistic select locking" );
			}
			return ConditionEvaluationResult.enabled( "READ_COMMITTED follow-on locking is eligible" );
		}
	}

	static void assertReadCommitted(SharedSessionContract session) {
		session.doWork( connection -> assertThat( connection.getTransactionIsolation() )
				.as( "effective reader/writer isolation" ).isEqualTo( Connection.TRANSACTION_READ_COMMITTED ) );
	}

	static void requireAcquisition(SessionFactoryScope scope) {
		final var concurrency = scope.getSessionFactory().getJdbcServices().getJdbcEnvironment().getTransactionConcurrency();
		assumeTrue( concurrency.supports( Operation.UPDATE_LOCK_READ ),
				"Resolved profile does not establish explicit update-lock-read support: " + concurrency.getName() );
	}

	static void requireDeletionRace(SessionFactoryScope scope) {
		requireAcquisition( scope );
		assumeTrue( new DialectFeatureChecks.SupportsConcurrentTransactions().apply( scope.getSessionFactory().getJdbcServices().getDialect() ),
				"Database profile does not support concurrent test transactions" );
		final var concurrency = scope.getSessionFactory().getJdbcServices().getJdbcEnvironment().getTransactionConcurrency();
		assumeTrue( concurrency.supports( Operation.READ ) && concurrency.supports( Operation.WRITE ),
				"Resolved profile does not establish ordinary-read and write support: " + concurrency.getName() );
		final var blocking = concurrency.getBlockingDuration( Operation.READ, Operation.WRITE );
		assumeTrue( blocking == BlockingDuration.NONE || blocking == BlockingDuration.STATEMENT,
				"Completed primary read may prevent the writer: " + blocking + " (" + concurrency.getName() + ")" );
		assumeTrue( concurrency.getReadGuarantees( Operation.UPDATE_LOCK_READ ).isCurrentRead(),
				"Resolved update-lock read does not establish current-read behavior: " + concurrency.getName() );
		// PESSIMISTIC_READ may use either a shared lock or an update-lock fallback.
		if ( concurrency.supports( Operation.SHARED_LOCK_READ ) ) {
			assumeTrue( concurrency.getReadGuarantees( Operation.SHARED_LOCK_READ ).isCurrentRead(),
					"Resolved shared-lock read does not establish current-read behavior: " + concurrency.getName() );
		}
	}

	static void assertFollowOnPlan(SharedSessionContractImplementor session, SelectionQuery<?> control) {
		final var query = (SelectionQueryImpl<?>) control;
		final var factory = session.getFactory();
		try {
			final var interpretation = factory.getQueryEngine().getSqmTranslatorFactory().createSelectTranslator(
					new SqmTranslationRequest.Select( query.getSqmStatement(), query.getQueryOptions(),
							query.getDomainParameterXref(), query.getParameterBindings(), session.getLoadQueryInfluencers(),
							factory.getSqlTranslationEngine(), true ) ).translate();
			final var operation = factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
					.buildTranslator( new SqlAstTranslationRequest.Select( factory, interpretation.getSqlAst() ) )
					.translate( JdbcParameterBindings.NO_BINDINGS, query.getQueryOptions() );
			assertThat( operation.getLockingApplication() ).as( "ordinary control must select follow-on acquisition" )
					.isEqualTo( JdbcLockingApplication.FOLLOW_ON );
			assertThat( operation.getLoadedValuesCollectorFactory() ).isNotNull();
		}
		finally {
			// Translation expands bound IN lists; leave the query ready for normal execution.
			query.getDomainParameterXref().clearExpansions();
		}
	}

	static boolean isEntitySelect(String sql, EntityPersister persister) {
		if ( !sql.stripLeading().toLowerCase( Locale.ROOT ).startsWith( "select" ) ) {
			return false;
		}
		final boolean[] matches = { false };
		persister.forEachTableDetails( table -> matches[0] |= sql.contains( table.getTableName() ) );
		return matches[0];
	}
}
