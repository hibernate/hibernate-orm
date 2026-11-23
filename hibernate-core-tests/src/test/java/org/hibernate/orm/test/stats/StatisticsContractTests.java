package org.hibernate.orm.test.stats;

import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

import org.hibernate.SessionEventListener;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Regression coverage for public cache-region and statement-release statistics.
///
/// @author Steve Ebersole
@BaseUnitTest
@JiraKey("HHH-20993")
class StatisticsContractTests {
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void domainRegionEnumerationExcludesQueryRegions(boolean sharedName) {
		try ( var factory = configuration()
				.property( "hibernate.cache.use_second_level_cache", true )
				.property( "hibernate.cache.use_query_cache", true )
				.property( "hibernate.cache.region.factory_class", "org.hibernate.testing.cache.CachingRegionFactory" )
				.createEntityManagerFactory() ) {
			factory.inTransaction( session -> session.persist( new Record( 1 ) ) );
			String queryRegion = sharedName ? "statistics-domain" : "statistics-query";
			factory.inTransaction( session -> assertThat( session.createQuery( "from StatisticsContractRecord", Record.class )
					.setCacheable( true ).setCacheRegion( queryRegion ).getResultList() ).hasSize( 1 ) );
			var statistics = factory.getStatistics();
			assertThat( statistics.getQueryRegionStatistics( "default-query-results-region" ) ).isNotNull();
			assertThat( statistics.getQueryRegionStatistics( queryRegion ) ).isNotNull();
			assertThat( statistics.getSecondLevelCacheRegionNames() ).containsExactly( "statistics-domain" );
		}
	}

	@Test
	void noRegionsWhenCachingDisabled() {
		try ( var factory = configuration().createEntityManagerFactory() ) {
			assertThat( factory.getStatistics().getSecondLevelCacheRegionNames() ).isEmpty();
		}
	}

	@Test
	void statementIsCountedWhenReleasedNotWhenPrepared() throws Exception {
		try ( var factory = configuration().createEntityManagerFactory();
				var session = factory.openSession() ) {
			var statistics = factory.getStatistics();
			statistics.clear();
			var coordinator = session.unwrap( SessionImplementor.class ).getJdbcCoordinator();
			var statement = coordinator.getStatementPreparer().prepareStatement( "select id from statistics_contract" );
			try {
				assertThat( statement.isClosed() ).isFalse();
				assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 1 );
				assertThat( statistics.getCloseStatementCount() ).isZero();
			}
			finally {
				coordinator.getLogicalConnection().getResourceRegistry().release( statement );
			}
			assertThat( statement.isClosed() ).isTrue();
			assertThat( statistics.getCloseStatementCount() ).isEqualTo( 1 );
		}
	}

	@Test
	void sessionCloseCountsOutstandingStatement() throws Exception {
		try ( var factory = configuration().createEntityManagerFactory() ) {
			var statistics = factory.getStatistics();
			statistics.clear();
			PreparedStatement statement;
			try ( var session = factory.openSession() ) {
				statement = session.unwrap( SessionImplementor.class ).getJdbcCoordinator()
						.getStatementPreparer().prepareStatement( "select id from statistics_contract" );
				assertThat( statistics.getCloseStatementCount() ).isZero();
			}
			assertThat( statement.isClosed() ).isTrue();
			assertThat( statistics.getCloseStatementCount() ).isEqualTo( 1 );
		}
	}

	@Test
	void reusedBatchStatementIsCountedOnceAtRelease() {
		try ( var factory = configuration().property( "hibernate.jdbc.batch_size", 5 ).createEntityManagerFactory() ) {
			assumeTrue( factory.unwrap( SessionFactoryImplementor.class ).getJdbcServices()
					.getExtractedMetaDataSupport().supportsBatchUpdates() );
			var statistics = factory.getStatistics();
			statistics.clear();
			var batches = new AtomicInteger();
			var listener = new SessionEventListener() {
				@Override
				public void jdbcExecuteBatchStart() {
					batches.incrementAndGet();
					assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 1 );
					assertThat( statistics.getCloseStatementCount() ).isZero();
				}
			};
			try ( var session = factory.withOptions().eventListeners( listener ).openSession() ) {
				var transaction = session.beginTransaction();
				session.persist( new Record( 1 ) );
				session.persist( new Record( 2 ) );
				session.persist( new Record( 3 ) );
				transaction.commit();
			}
			assertThat( batches.get() ).isEqualTo( 1 );
			assertThat( statistics.getEntityInsertCount() ).isEqualTo( 3 );
			assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 1 );
			assertThat( statistics.getCloseStatementCount() ).isEqualTo( 1 );
			factory.inTransaction( session -> assertThat( session.createQuery( "from StatisticsContractRecord", Record.class )
					.getResultList() ).hasSize( 3 ) );
		}
	}

	private static HibernatePersistenceConfiguration configuration() {
		return new HibernatePersistenceConfiguration( "statistics-contract" )
				.managedClasses( Record.class )
				.properties( ServiceRegistryUtil.createBaseSettingsWithExport() )
				.property( "hibernate.cache.use_second_level_cache", false )
				.property( "hibernate.cache.use_query_cache", false )
				.property( "hibernate.generate_statistics", true );
	}

	@Test
	void tableGeneratorCountsStatementsWhenClosed() {
		try ( var factory = configuration().managedClasses( GeneratedRecord.class ).createEntityManagerFactory() ) {
			var statistics = factory.getStatistics();
			statistics.clear();
			var executions = new AtomicInteger();
			var listener = new SessionEventListener() {
				@Override
				public void jdbcExecuteStatementStart() {
					executions.incrementAndGet();
					assertThat( statistics.getCloseStatementCount() ).isLessThan( statistics.getPrepareStatementCount() );
				}
			};
			try ( var session = factory.withOptions().eventListeners( listener ).openSession() ) {
				var transaction = session.beginTransaction();
				var record = new GeneratedRecord();
				session.persist( record );
				assertThat( record.id ).isNotNull();
				assertThat( executions.get() ).isGreaterThan( 0 );
				transaction.commit();
			}
			assertThat( statistics.getCloseStatementCount() ).isEqualTo( statistics.getPrepareStatementCount() );
			factory.inTransaction( session -> assertThat( session.createQuery( "from StatisticsGeneratedRecord", GeneratedRecord.class )
					.getResultList() ).hasSize( 1 ) );
		}
	}

	@Entity(name = "StatisticsGeneratedRecord")
	@Table(name = "statistics_generated_record")
	public static class GeneratedRecord {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "statistics-generator")
		@TableGenerator(name = "statistics-generator", table = "statistics_generator",
				pkColumnName = "segment", valueColumnName = "next_value", allocationSize = 1)
		Long id;
	}

	@Entity(name = "StatisticsContractRecord")
	@Table(name = "statistics_contract")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "statistics-domain")
	public static class Record {
		@Id
		int id;

		public Record() {
		}

		Record(int id) {
			this.id = id;
		}
	}
}
