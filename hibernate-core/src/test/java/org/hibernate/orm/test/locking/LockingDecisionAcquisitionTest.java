package org.hibernate.orm.test.locking;

import java.util.Map;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Locking;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.internal.SelectionQueryImpl;
import org.hibernate.query.sqm.sql.spi.SqmTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Database-backed counterpart of
/// `org.hibernate.orm.test.dialect.unit.locking.LockingDecisionBaselineTest`,
/// for the locking findings in
/// `design/working/sql-ast-translator-convention-proposal.adoc`.
///
/// Each test runs a locked HQL query in one transaction and, while that
/// transaction is open, probes from a second transaction whether the rows are
/// protected against writes. The probe is the skip-locked SELECT used by the
/// other locking tests in this package: a skipped row is a protected row.
///
/// These run against whichever database the build is configured for. A test
/// whose finding depends on a capability the database lacks states that as an
/// assumption.
///
/// Tests marked `@FailureExpected` state the intended behavior and fail today.
/// The build verifies that they still fail, but it cannot tell why: a failed
/// precondition or an unmet assumption satisfies the marker just as the real
/// defect does. So these tests carry no precondition assertions of their own;
/// `cteQueryLocksTheSelectedRows` is the unmarked test that proves the CTE is
/// rendered, and each marker says on which database its failure was observed.
@DomainModel(annotatedClasses = {
		LockingDecisionAcquisitionTest.Owner.class,
		LockingDecisionAcquisitionTest.Item.class
})
@SessionFactory
@ExtendWith(FollowOnLockingTestSupport.ReadCommittedEligibility.class)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@Tag("db-locking")
class LockingDecisionAcquisitionTest {
	// The CTE is recursive so that it is rendered as a `with` clause everywhere:
	// H2 inlines every non-recursive CTE, which would bypass the path under test.
	// It yields the single id 1.
	private static final String CTE =
			"with ids as (select 1 as id union all select c.id + 1 as id from ids c where c.id < 1) ";
	private static final String CTE_QUERY =
			CTE + "select i from AcquisitionItem i where i.id in (select c.id from ids c)";

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var owner = new Owner( 1 );
			session.persist( owner );
			session.persist( new Item( 1, owner ) );
			session.persist( new Item( 2, owner ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void plainQueryLocksTheSelectedRows(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		inReader( scope, reader -> {
			final var query = reader.createQuery( "select i from AcquisitionItem i where i.id = 1", Item.class )
					.setLockMode( PESSIMISTIC_WRITE );
			assertThat( query.getResultList() ).hasSize( 1 );
			assertItemProtection( scope, 1, true );
			assertItemProtection( scope, 2, false );
		} );
	}

	@Test
	void cteQueryLocksTheSelectedRows(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		inReader( scope, reader -> {
			final var query = reader.createQuery( CTE_QUERY, Item.class ).setLockMode( PESSIMISTIC_WRITE );
			assertThat( translate( reader, query ).getSqlString() ).as( "CTE rendered, not inlined" ).startsWith( "with " );
			assertThat( query.getResultList() ).hasSize( 1 );
			assertItemProtection( scope, 1, true );
			assertItemProtection( scope, 2, false );
		} );
	}

	@Test
	@FailureExpected(reason = "Finding 1: the locking strategy is built from the CTE's query spec, so the clause"
			+ " names no lock target and every table in the query is locked. NOT verified against a"
			+ " database: H2 cannot restrict a locking clause to selected tables, so there this test"
			+ " stops at its assumption, which also satisfies this marker.")
	void cteQueryDoesNotLockAnUnselectedJoinedTable(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		assumeTrue( scope.getSessionFactory().getJdbcServices().getDialect().getLockingSupport().getMetadata()
						.getWriteRowLockStrategy() != RowLockStrategy.NONE,
				"Database cannot restrict a locking clause to selected tables" );
		inReader( scope, reader -> {
			final var query = reader.createQuery(
							CTE + "select i from AcquisitionItem i join i.owner o "
									+ "where i.id in (select c.id from ids c)", Item.class )
					.setLockMode( PESSIMISTIC_WRITE );
			assertThat( query.getResultList() ).hasSize( 1 );
			assertItemProtection( scope, 1, true );
			assertThat( probeWriteProtection( scope, "acquisition_owner", 1 ) )
					.as( "owner row is joined but not selected, so it is not a lock target" ).isEqualTo( 1 );
		} );
	}

	@Test
	// SQL Server and Sybase lock directly even when follow-on locking is forced, so
	// they never reach the follow-on path and this test would pass there.
	@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
	@FailureExpected(reason = "Finding 1: the locking strategy is built from the CTE's query spec, so"
			+ " getPathsToLock() is null and follow-on locking fails with a NullPointerException"
			+ " while processing results. Verified on H2.")
	void cteQueryWithForcedFollowOnLocksTheSelectedRows(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		inReader( scope, reader -> {
			final var query = reader.createQuery( CTE_QUERY, Item.class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE );
			assertThat( query.getResultList() ).hasSize( 1 );
			assertItemProtection( scope, 1, true );
			assertItemProtection( scope, 2, false );
		} );
	}

	@Test
	@FailureExpected(reason = "Finding 9: the locking clause is rendered at the end of the first branch of a set"
			+ " operation. Verified on H2, where the statement fails with a syntax error.")
	void setOperationQueryLocksEveryBranch(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		inReader( scope, reader -> {
			final var query = reader.createQuery(
							"select i from AcquisitionItem i where i.id = 1 "
									+ "union all select i from AcquisitionItem i where i.id = 2", Item.class )
					.setLockMode( PESSIMISTIC_WRITE );
			assertThat( query.getResultList() ).hasSize( 2 );
			assertItemProtection( scope, 1, true );
			assertItemProtection( scope, 2, true );
		} );
	}

	@Test
	void ignoredLockingAcquiresNoLocks(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		inReader( scope, reader -> {
			// DISTINCT cannot be combined with a locking clause; IGNORE then asks for no locking at all
			final var query = reader.createQuery( "select distinct i from AcquisitionItem i where i.id = 1", Item.class )
					.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.IGNORE );
			final var sql = translate( reader, query ).getSqlString();
			assertThat( query.getResultList() ).hasSize( 1 );
			assertThat( probeWriteProtection( scope, "acquisition_item", 1 ) )
					.as( "no lock requested by: %s", sql ).isEqualTo( 1 );
		} );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Support

	private static void inReader(SessionFactoryScope scope, Consumer<SessionImplementor> action) {
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				FollowOnLockingTestSupport.assertReadCommitted( session );
				action.accept( session.unwrap( SessionImplementor.class ) );
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
	}

	private static void assertItemProtection(SessionFactoryScope scope, int id, boolean protectedRow) {
		assertThat( probeWriteProtection( scope, "acquisition_item", id ) )
				.as( "item %s write protection", id ).isEqualTo( protectedRow ? 0 : 1 );
	}

	/// Returns 0 when the row is protected by another transaction's lock, 1 when it is not.
	private static int probeWriteProtection(SessionFactoryScope scope, String table, int id) {
		return scope.fromTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var sql = FollowOnLockingTestSupport.writeProtectionProbeSql( session,
					"select 1 from " + table + " t where id = " + id,
					Map.of( "t", new String[] { "id" } ) );
			return session.createNativeQuery( sql, Integer.class ).getResultList().size();
		} );
	}

	/// Translates without executing, to confirm the SQL shape a test exercises.
	private static JdbcSelect translate(SessionImplementor session, SelectionQuery<?> control) {
		final var query = (SelectionQueryImpl<?>) control;
		final var factory = session.getFactory();
		try {
			final var interpretation = factory.getQueryEngine().getSqmTranslatorFactory().createSelectTranslator(
					new SqmTranslationRequest.Select( query.getSqmStatement(), query.getQueryOptions(),
							query.getDomainParameterXref(), query.getParameterBindings(), session.getLoadQueryInfluencers(),
							factory.getSqlTranslationEngine(), true ) ).translate();
			return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
					.buildTranslator( new SqlAstTranslationRequest.Select( factory, interpretation.getSqlAst() ) )
					.translate( JdbcParameterBindings.NO_BINDINGS, query.getQueryOptions() );
		}
		finally {
			query.getDomainParameterXref().clearExpansions();
		}
	}

	@Entity(name = "AcquisitionOwner")
	@Table(name = "acquisition_owner")
	static class Owner {
		@Id int id;
		String name;

		Owner() {
		}

		Owner(int id) {
			this.id = id;
			this.name = "owner";
		}
	}

	@Entity(name = "AcquisitionItem")
	@Table(name = "acquisition_item")
	static class Item {
		@Id int id;
		@Version int version;
		String name;
		@ManyToOne(fetch = FetchType.LAZY) Owner owner;

		Item() {
		}

		Item(int id, Owner owner) {
			this.id = id;
			this.owner = owner;
			this.name = "item";
		}
	}
}
