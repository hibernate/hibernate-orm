package org.hibernate.orm.test.stats;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Executable examples of isolated factory and persistence-context measurements.
///
/// @author Steve Ebersole
@BaseUnitTest
public class StatisticsGuideTests {

	@Test
	void accessStatistics() {
		var configuration = configuration();
		// tag::statistics-access[]
		configuration.property( "hibernate.generate_statistics", true );
		try ( EntityManagerFactory entityManagerFactory = configuration.createEntityManagerFactory() ) {
			var factory = entityManagerFactory.unwrap( SessionFactory.class );
			var statistics = factory.getStatistics();
			Instant recordingSince = statistics.getStart();
			assertThat( statistics.isStatisticsEnabled() ).isTrue();
			assertThat( recordingSince ).isNotNull();
		}
		// end::statistics-access[]
	}

	@Test
	void collectionLifecycle() {
		try ( var factory = configuration().createEntityManagerFactory() ) {
			var statistics = factory.getStatistics();
			Instant recordingSince = statistics.getStart();
			factory.runInTransaction( manager -> manager.persist( new Record( 1, "first" ) ) );
			assertThat( statistics.getEntityInsertCount() ).isEqualTo( 1 );
			statistics.setStatisticsEnabled( false );
			factory.runInTransaction( manager -> manager.persist( new Record( 2, "disabled" ) ) );
			assertThat( statistics.getEntityInsertCount() ).isEqualTo( 1 );
			statistics.setStatisticsEnabled( true );
			factory.runInTransaction( manager -> manager.persist( new Record( 3, "enabled" ) ) );
			assertThat( statistics.getEntityInsertCount() ).isEqualTo( 2 );
			statistics.clear();
			assertThat( statistics.getEntityInsertCount() ).isZero();
			assertThat( statistics.getStart() ).isAfterOrEqualTo( recordingSince );
		}
	}

	@Test
	void controlledMeasurementAndDeltas() {
		try ( var factory = configuration().createEntityManagerFactory() ) {
			// tag::statistics-measurement[]
			factory.runInTransaction( manager -> manager.persist( new Record( 1, "first" ) ) );

			var statistics = factory.getStatistics();
			statistics.clear(); // Setup is outside the measurement interval.
			factory.runInTransaction( manager -> {
				var records = manager.createQuery( "from StatisticsRecord", Record.class ).getResultList();
				assertThat( records ).extracting( record -> record.label ).containsExactly( "first" );
			} );
			assertThat( statistics.getQueryExecutionCount() ).isEqualTo( 1 );
			assertThat( statistics.getEntityLoadCount() ).isEqualTo( 1 );
			assertThat( statistics.getEntityStatistics( Record.class.getName() ).getLoadCount() ).isEqualTo( 1 );

			long loadsBefore = statistics.getEntityLoadCount();
			factory.runInTransaction( manager -> {
				assertThat( manager.find( Record.class, 1 ).label ).isEqualTo( "first" );
			} );
			long additionalLoads = statistics.getEntityLoadCount() - loadsBefore;
			assertThat( additionalLoads ).isEqualTo( 1 );
			// end::statistics-measurement[]
		}
	}

	@Test
	void perQueryMeasurements() {
		try ( var factory = configuration().createEntityManagerFactory() ) {
			factory.runInTransaction( manager -> manager.persist( new Record( 1, "first" ) ) );
			// tag::statistics-query[]
			var statistics = factory.getStatistics();
			statistics.clear();
			String hql = "from StatisticsRecord where label = :label";
			factory.runInTransaction( manager -> {
				var records = manager.createQuery( hql, Record.class )
						.setParameter( "label", "first" )
						.getResultList();
				assertThat( records ).extracting( record -> record.id ).containsExactly( 1 );
			} );

			var queryStatistics = statistics.getQueryStatistics( hql );
			assertThat( queryStatistics.getExecutionCount() ).isEqualTo( 1 );
			assertThat( queryStatistics.getExecutionRowCount() ).isEqualTo( 1 );
			long totalMilliseconds = queryStatistics.getExecutionTotalTime();
			double averageMilliseconds = queryStatistics.getExecutionAvgTimeAsDouble();
			assertThat( totalMilliseconds ).isGreaterThanOrEqualTo( 0 );
			assertThat( averageMilliseconds ).isEqualTo( (double) totalMilliseconds );
			// end::statistics-query[]
		}
	}

	@Test
	@SuppressWarnings("removal") // Also verify the legacy executeUpdate() path.
	void bulkMutationsDoNotPopulateExecutionOrEntityMutationCounters() {
		try ( var factory = configuration().createEntityManagerFactory() ) {
			factory.runInTransaction( manager -> {
				manager.persist( new Record( 1, "first" ) );
				manager.persist( new Record( 2, "second" ) );
			} );
			var statistics = factory.getStatistics();
			statistics.clear();
			String hqlUpdate = "update StatisticsRecord set label = :label where id = 1";
			String hqlDelete = "delete StatisticsRecord where id = 2";
			factory.runInTransaction( manager -> {
				assertThat( manager.createStatement( hqlUpdate ).setParameter( "label", "HQL" ).execute() )
						.isEqualTo( 1 );
				assertThat( manager.createQuery( hqlDelete ).executeUpdate() ).isEqualTo( 1 );
				var update = manager.getCriteriaBuilder().createCriteriaUpdate( Record.class );
				update.from( Record.class );
				update.set( "label", "Criteria" );
				assertThat( manager.createStatement( update ).execute() ).isEqualTo( 1 );
				assertThat( manager.createNativeStatement( "update statistics_record set label = 'native' where id = 1" )
						.execute() ).isEqualTo( 1 );
			} );
			assertThat( statistics.getQueryExecutionCount() ).isZero();
			assertThat( statistics.getQueryExecutionMaxTime() ).isZero();
			assertThat( statistics.getEntityInsertCount() ).isZero();
			assertThat( statistics.getEntityUpdateCount() ).isZero();
			assertThat( statistics.getEntityDeleteCount() ).isZero();
			for ( String hql : new String[] { hqlUpdate, hqlDelete } ) {
				assertThat( statistics.getQueryStatistics( hql ).getExecutionCount() ).isZero();
				assertThat( statistics.getQueryStatistics( hql ).getExecutionTotalTime() ).isZero();
			}
			factory.runInTransaction( manager -> {
				assertThat( manager.find( Record.class, 1 ).label ).isEqualTo( "native" );
				assertThat( manager.find( Record.class, 2 ) ).isNull();
			} );
		}
	}

	@Test
	void planCacheBeforeExecution() {
		// This factory has not previously compiled or executed the example query.
		try ( var factory = configuration().createEntityManagerFactory() ) {
			// tag::statistics-plan-cache[]
			var statistics = factory.getStatistics();
			String hql = "from StatisticsRecord where label = :label";
			try ( var manager = factory.createEntityManager() ) {
				manager.createQuery( hql, Record.class );
				assertThat( statistics.getQueryPlanCacheMissCount() ).isGreaterThan( 0 );
				assertThat( statistics.getQueryExecutionCount() ).isZero();

				long hitsBefore = statistics.getQueryPlanCacheHitCount();
				manager.createQuery( hql, Record.class );
				assertThat( statistics.getQueryPlanCacheHitCount() ).isGreaterThan( hitsBefore );
				assertThat( statistics.getQueryStatistics( hql ).getPlanCacheHitCount() ).isGreaterThan( 0 );
				assertThat( statistics.getQueryExecutionCount() ).isZero();
			}
			// end::statistics-plan-cache[]
			long missesBefore = statistics.getQueryPlanCacheMissCount();
			statistics.clear();
			assertThat( missesBefore ).isGreaterThan( 0 );
			try ( var manager = factory.createEntityManager() ) {
				manager.createQuery( hql, Record.class );
				assertThat( statistics.getQueryPlanCacheMissCount() ).isZero();
				assertThat( statistics.getQueryPlanCacheHitCount() ).isGreaterThan( 0 );
			}
		}
	}

	@Test
	void persistenceContextMeasurements() {
		try ( var factory = configuration().createEntityManagerFactory() ) {
			factory.runInTransaction( manager -> manager.persist( new Record( 1, "first" ) ) );
			factory.getStatistics().clear();
			// tag::statistics-session[]
			factory.inTransaction( session -> {
				session.find( Record.class, 1 );
				var contextStatistics = session.getStatistics();
				assertThat( contextStatistics.getEntityCount() ).isEqualTo( 1 );
				assertThat( factory.getStatistics().getEntityLoadCount() ).isEqualTo( 1 );

				session.clear();
				assertThat( contextStatistics.getEntityCount() ).isZero();
				assertThat( factory.getStatistics().getEntityLoadCount() ).isEqualTo( 1 );
			} );
			// end::statistics-session[]
		}
	}

	@Test
	void resetReplacesQueryDetail() {
		try ( var factory = configuration().createEntityManagerFactory() ) {
			factory.runInTransaction( manager -> manager.persist( new Record( 1, "first" ) ) );
			var statistics = factory.getStatistics();
			String hql = "from StatisticsRecord";
			factory.runInTransaction( manager -> manager.createQuery( hql, Record.class ).getResultList() );
			var beforeReset = statistics.getQueryStatistics( hql );
			assertThat( beforeReset.getExecutionCount() ).isEqualTo( 1 );
			statistics.clear();
			assertThat( statistics.getQueries() ).isEmpty();
			var afterReset = statistics.getQueryStatistics( hql );
			assertThat( afterReset ).isNotSameAs( beforeReset );
			assertThat( afterReset.getExecutionCount() ).isZero();
			assertThat( beforeReset.getExecutionCount() ).isEqualTo( 1 );
			assertThat( statistics.getQueries() ).contains( hql );
		}
	}

	@Test
	void evictionLosesDetailButRetainsFactoryTotals() {
		try ( var factory = configuration()
				.property( "hibernate.statistics.query_max_size", 100 )
				.createEntityManagerFactory() ) {
			factory.runInTransaction( manager -> manager.persist( new Record( 1, "first" ) ) );
			var statistics = factory.getStatistics();
			String hql = "from StatisticsRecord";
			factory.runInTransaction( manager -> manager.createQuery( hql, Record.class ).getResultList() );
			var original = statistics.getQueryStatistics( hql );
			assertThat( original.getExecutionCount() ).isEqualTo( 1 );
			// Explicit detail lookups also populate the bounded container, without execution.
			for ( int i = 0; i < 2000; i++ ) {
				statistics.getQueryStatistics( "unexecuted-" + i );
			}
			assertThat( statistics.getQueries() ).doesNotContain( hql );
			assertThat( statistics.getQueryExecutionCount() ).isEqualTo( 1 );
			var replacement = statistics.getQueryStatistics( hql );
			assertThat( replacement ).isNotSameAs( original );
			assertThat( replacement.getExecutionCount() ).isZero();
			assertThat( original.getExecutionCount() ).isEqualTo( 1 );
		}
	}

	@Test
	void rollbackDoesNotUndoMutationCounters() {
		try ( var factory = configuration().createEntityManagerFactory() ) {
			var statistics = factory.getStatistics();
			try ( var manager = factory.createEntityManager() ) {
				var transaction = manager.getTransaction();
				transaction.begin();
				manager.persist( new Record( 1, "rolled back" ) );
				manager.flush();
				assertThat( statistics.getEntityInsertCount() ).isEqualTo( 1 );
				transaction.rollback();
			}
			assertThat( statistics.getEntityInsertCount() ).isEqualTo( 1 );
			assertThat( statistics.getFailedTransactionCount() ).isEqualTo( 1 );
			factory.runInTransaction( manager -> assertThat( manager.find( Record.class, 1 ) ).isNull() );
		}
	}

	private static HibernatePersistenceConfiguration configuration() {
		return new HibernatePersistenceConfiguration( "statistics-guide" )
				.managedClasses( Record.class )
				.properties( ServiceRegistryUtil.createBaseSettingsWithExport() )
				.property( "hibernate.cache.use_second_level_cache", false )
				.property( "hibernate.cache.use_query_cache", false )
				.property( "hibernate.generate_statistics", true );
	}

	@Entity(name = "StatisticsRecord")
	@Table(name = "statistics_record")
	public static class Record {
		@Id
		int id;
		String label;

		public Record() {
		}

		Record(int id, String label) {
			this.id = id;
			this.label = label;
		}
	}
}
