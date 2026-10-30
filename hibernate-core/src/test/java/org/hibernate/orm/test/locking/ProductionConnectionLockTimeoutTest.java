package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;

import org.hibernate.Locking;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.spi.InterpretationsKeySource;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Verifies execution-local timeout restoration using production dialect routing
/// and connection commands, including overlapping uses of a cached select plan.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = ProductionConnectionLockTimeoutTest.Item.class)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false"),
		@Setting(name = "hibernate.query.plan_cache_enabled", value = "true")
})
@ExtendWith(ProductionConnectionLockTimeoutTest.Eligibility.class)
@Tag("db-locking")
@Jira("https://hibernate.atlassian.net/browse/HHH-21032")
public class ProductionConnectionLockTimeoutTest {
	private static final String HQL = "from ProductionTimeoutItem where id = :id";
	private static final Timeout REQUEST = Timeout.seconds( 1 );
	private static final Timeout FIRST_BASELINE = Timeout.seconds( 2 );
	private static final Timeout SECOND_BASELINE = Timeout.seconds( 4 );

	/// Excludes unsupported production routing before constructing the fixture.
	public static class Eligibility implements ExecutionCondition {
		@Override
		public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
			final var dialectClass = DialectContext.getDialectClass();
			if ( SybaseDialect.class.isAssignableFrom( dialectClass ) ) {
				return ConditionEvaluationResult.disabled( "Sybase pessimistic-write hints do not establish update-lock-read support" );
			}
			if ( SpannerDialect.class.isAssignableFrom( dialectClass ) || SpannerPostgreSQLDialect.class.isAssignableFrom( dialectClass ) ) {
				return ConditionEvaluationResult.disabled( "Database profile does not support READ_COMMITTED" );
			}
			final var dialect = DialectContext.getDialect();
			if ( dialect instanceof CockroachDialect && dialect.getVersion().isBefore( 23, 2 ) ) {
				return ConditionEvaluationResult.disabled( "Database profile does not support READ_COMMITTED" );
			}
			final var support = dialect.getLockingSupport();
			if ( support.getMetadata().getPessimisticLockStyle() == PessimisticLockStyle.NONE ) {
				return ConditionEvaluationResult.disabled( "Dialect does not support pessimistic select locking" );
			}
			if ( support.getConnectionLockTimeoutStrategy().getSupportedLevel() == ConnectionLockTimeoutStrategy.Level.NONE ) {
				return ConditionEvaluationResult.disabled( "Production dialect has no connection lock-timeout strategy" );
			}
			final var routing = support.getMetadata().getLockTimeoutType( REQUEST );
			if ( routing != LockTimeoutType.CONNECTION ) {
				return ConditionEvaluationResult.disabled( "Production finite lock-timeout routing is " + routing + ", not CONNECTION" );
			}
			return ConditionEvaluationResult.enabled( "Production connection lock-timeout routing is supported" );
		}
	}

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			session.persist( new Item( 1 ) );
			session.persist( new Item( 2 ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void overlappingCachedQueriesRestoreTheirOwnBaselines(SessionFactoryScope scope) {
		assumeTrue( new DialectFeatureChecks.SupportsConcurrentTransactions().apply( scope.getSessionFactory().getJdbcServices().getDialect() ),
				"Database profile does not support concurrent test transactions" );
		final var warmedPlan = new AtomicReference<SelectQueryPlan<Item>>();
		withExecution( scope, FIRST_BASELINE, execution -> execution.assertTimeout( REQUEST ), execution -> {
			final var query = execution.query( 1 );
			assertThat( query.getSingleResult().id ).isEqualTo( 1 );
			warmedPlan.set( cachedPlan( execution, query ) );
			assertThat( execution.selects ).isEqualTo( 1 );
		} );

		final var firstInQuery = new CountDownLatch( 1 );
		final var secondInQuery = new CountDownLatch( 1 );
		final var firstCompleted = new CountDownLatch( 1 );
		final var firstConnection = new AtomicReference<Connection>();
		final var executor = Executors.newFixedThreadPool( 2 );
		try ( var shutdown = new Cleanup( () -> {
			firstInQuery.countDown();
			secondInQuery.countDown();
			firstCompleted.countDown();
			executor.shutdownNow();
			try {
				assertThat( executor.awaitTermination( 30, TimeUnit.SECONDS ) ).isTrue();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException( e );
			}
		} ) ) {
			final var first = executor.submit( () -> {
				try {
					withExecution( scope, FIRST_BASELINE, execution -> {
						execution.assertTimeout( REQUEST );
						firstConnection.set( execution.connection );
						firstInQuery.countDown();
						await( secondInQuery );
					}, execution -> executeCached( execution, 1, warmedPlan.get() ) );
				}
				finally {
					firstInQuery.countDown();
					firstCompleted.countDown();
				}
			} );
			final var second = executor.submit( () -> {
				try {
					await( firstInQuery );
					withExecution( scope, SECOND_BASELINE, execution -> {
						execution.assertTimeout( REQUEST );
						assertThat( firstConnection.get() ).isNotNull().isNotSameAs( execution.connection );
						secondInQuery.countDown();
						await( firstCompleted );
					}, execution -> executeCached( execution, 2, warmedPlan.get() ) );
				}
				finally {
					secondInQuery.countDown();
				}
			} );
			assertAll( () -> first.get( 30, TimeUnit.SECONDS ), () -> second.get( 30, TimeUnit.SECONDS ) );
		}
	}

	@Test
	void statementPreparationFailureRestoresBaseline(SessionFactoryScope scope) {
		final var original = new IllegalStateException( "statement preparation" );
		withExecution( scope, FIRST_BASELINE, execution -> {
			execution.assertTimeout( REQUEST );
			throw original;
		}, execution -> {
			assertThatThrownBy( () -> execution.query( 1 ).getSingleResult() ).isSameAs( original );
			assertThat( execution.selects ).isEqualTo( 1 );
			assertThat( original.getSuppressed() ).isEmpty();
		} );
	}

	@Test
	void resultConsumptionFailureRestoresBaseline(SessionFactoryScope scope) {
		final var original = new IllegalStateException( "row transformation" );
		withExecution( scope, FIRST_BASELINE, execution -> execution.assertTimeout( REQUEST ), execution -> {
			assertThatThrownBy( () -> execution.query( 1 ).setTupleTransformer( (tuple, aliases) -> {
				assertThat( ((Item) tuple[0]).id ).isEqualTo( 1 );
				execution.assertTimeout( REQUEST );
				throw original;
			} ).getSingleResult() ).isSameAs( original );
			assertThat( execution.selects ).isEqualTo( 1 );
			assertThat( original.getSuppressed() ).isEmpty();
		} );
	}

	@Test
	@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
	@Jira("https://hibernate.atlassian.net/browse/HHH-21027")
	void missingFollowOnRowRestoresBaseline(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireDeletionRace( scope );
		withExecution( scope, FIRST_BASELINE, execution -> {
			execution.assertTimeout( REQUEST );
			if ( execution.selects == 2 ) {
				scope.inTransaction( writer -> {
					FollowOnLockingTestSupport.assertReadCommitted( writer );
					assertThat( writer.createMutationQuery( "delete from ProductionTimeoutItem where id = 1" ).executeUpdate() )
							.isEqualTo( 1 );
				} );
			}
		}, execution -> {
			final var query = execution.query( 1 ).setFollowOnStrategy( Locking.FollowOn.FORCE );
			FollowOnLockingTestSupport.assertFollowOnPlan( execution.session, query );
			assertThatThrownBy( query::getSingleResult ).isInstanceOf( OptimisticLockException.class )
					.hasCauseInstanceOf( StaleObjectStateException.class ).satisfies( failure -> {
						final var stale = (StaleObjectStateException) failure.getCause();
						assertThat( stale.getEntityName() ).isEqualTo( Item.class.getName() );
						assertThat( stale.getIdentifier() ).isEqualTo( 1 );
						assertThat( stale.getSuppressed() ).isEmpty();
					} );
			assertThat( execution.selects ).isEqualTo( 2 );
		} );
	}

	private static void executeCached(Execution execution, int id, SelectQueryPlan<Item> warmedPlan) {
		final var query = execution.query( id );
		assertThat( cachedPlan( execution, query ) ).isSameAs( warmedPlan );
		assertThat( query.getSingleResult().id ).isEqualTo( id );
		assertThat( cachedPlan( execution, query ) ).isSameAs( warmedPlan );
		assertThat( execution.selects ).isEqualTo( 1 );
	}

	private static SelectQueryPlan<Item> cachedPlan(Execution execution, SelectionQuery<Item> query) {
		final var cache = execution.session.getFactory().getQueryEngine().getInterpretationCache();
		assertThat( cache.isEnabled() ).isTrue();
		final var key = QueryInterpretationCache.createInterpretationsKey( (InterpretationsKeySource) query );
		assertThat( key ).as( "cacheable select query" ).isNotNull();
		return cache.resolveSelectQueryPlan( key, () -> {
			throw new AssertionError( "Expected the warmed select plan in the interpretation cache" );
		} );
	}

	private static void withExecution(
			SessionFactoryScope scope, Timeout baseline, Consumer<Execution> onSelect, Consumer<Execution> work) {
		final var reference = new AtomicReference<Execution>();
		final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Item.class );
		final UnaryOperator<String> inspector = sql -> {
			if ( FollowOnLockingTestSupport.isEntitySelect( sql, persister ) ) {
				final var execution = reference.get();
				execution.selects++;
				onSelect.accept( execution );
			}
			return sql;
		};
		try ( var session = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			final var transaction = session.beginTransaction();
			// Separate resources preserve failures from restoration and rollback as suppressed exceptions.
			try ( var rollback = new Cleanup( () -> {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			} ) ) {
				FollowOnLockingTestSupport.assertReadCommitted( session );
				final var execution = new Execution( session.unwrap( SessionImplementor.class ) );
				reference.set( execution );
				final var original = execution.readTimeout();
				try ( var restore = new Cleanup( () -> execution.setTimeout( original ) ) ) {
					execution.setTimeout( baseline );
					work.accept( execution );
					// Check before rollback: transaction completion could otherwise conceal missing SET LOCAL restoration.
					execution.assertTimeout( baseline );
					assertThat( execution.session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources() )
							.isFalse();
				}
			}
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			assertThat( latch.await( 10, TimeUnit.SECONDS ) ).as( "execution interleaving reached" ).isTrue();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException( e );
		}
	}

	private static class Execution {
		private final SessionImplementor session;
		private final Connection connection;
		private final ConnectionLockTimeoutStrategy strategy;
		private int selects;

		private Execution(SessionImplementor session) {
			this.session = session;
			connection = session.doReturningWork( jdbcConnection -> jdbcConnection );
			strategy = session.getJdbcServices().getDialect().getLockingSupport().getConnectionLockTimeoutStrategy();
		}

		private Timeout readTimeout() {
			return session.doReturningWork( jdbcConnection -> {
				assertThat( jdbcConnection ).as( "transaction-bound connection" ).isSameAs( connection );
				return strategy.getLockTimeout( jdbcConnection, session.getFactory() );
			} );
		}

		private void setTimeout(Timeout timeout) {
			session.doWork( jdbcConnection -> {
				assertThat( jdbcConnection ).as( "transaction-bound connection" ).isSameAs( connection );
				strategy.setLockTimeout( timeout, jdbcConnection, session.getFactory() );
			} );
			assertTimeout( timeout );
		}

		private void assertTimeout(Timeout timeout) {
			assertThat( readTimeout().milliseconds() ).as( "connection lock timeout before transaction completion" )
					.isEqualTo( timeout.milliseconds() );
		}

		private SelectionQuery<Item> query(int id) {
			return session.createQuery( HQL, Item.class ).setParameter( "id", id )
					.setLockMode( PESSIMISTIC_WRITE ).setLockTimeout( REQUEST ).setFollowOnStrategy( Locking.FollowOn.DISALLOW );
		}
	}

	private record Cleanup(Runnable action) implements AutoCloseable {
		@Override
		public void close() {
			action.run();
		}
	}

	@Entity(name = "ProductionTimeoutItem")
	@Table(name = "production_timeout_item")
	static class Item {
		@Id int id;
		Item() {}
		Item(int id) { this.id = id; }
	}
}
