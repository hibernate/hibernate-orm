/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultcache;

import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.sql.exec.SqlExecLogger;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.logger.LogLevelContext.withLevel;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true")
		}
)
@DomainModel(annotatedClasses = { TestEntity.class, AggregateEntity.class })
@SessionFactory(generateStatistics = true)
public class QueryResultCacheTests {

	Logger resultsLogger = ResultsLogger.RESULTS_LOGGER;

	@Test
	public void testScalarCaching(SessionFactoryScope scope) {
		try (var l1 = withLevel( ResultsLogger.LOGGER_NAME, Logger.Level.TRACE );
			var l2 = withLevel( SqlExecLogger.LOGGER_NAME, Logger.Level.TRACE );
			var l3 = withLevel( SecondLevelCacheLogger.LOGGER_NAME, Logger.Level.TRACE );) {

			final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
			statistics.clear();

			assertThat( statistics.getPrepareStatementCount(), is( 0L ) );

			final String hql = "select e.id, e.name from TestEntity e order by e.id";

			scope.inTransaction(
					session -> {
						resultsLogger.debug( "First query ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );

						final List values = session.createQuery( hql )
								.setCacheable( true )
								.setCacheMode( CacheMode.NORMAL )
								.setCacheRegion( "scalar-region" )
								.list();

						assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

						verifyScalarResults( values );
					}
			);

			scope.inTransaction(
					session -> {
						resultsLogger.debug( "Second query ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );

						final List values = session.createQuery( hql )
								.setCacheable( true )
								.setCacheMode( CacheMode.NORMAL )
								.setCacheRegion( "scalar-region" )
								.list();

						assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

						verifyScalarResults( values );
					}
			);
		}
	}

	private void verifyScalarResults(List values) {
		assertThat( values.size(), is( 2 ) );
		final Object[] firstRow = (Object[]) values.get( 0 );
		assertThat( firstRow[0], is( 1 ) );
		assertThat( firstRow[1], is( "first" ) );
		final Object[] secondRow = (Object[]) values.get( 1 );
		assertThat( secondRow[0], is( 2 ) );
		assertThat( secondRow[1], is( "second" ) );
	}

	@Test
	public void testJoinFetchCaching(SessionFactoryScope scope) {
		try (var l1 = withLevel( ResultsLogger.LOGGER_NAME, Logger.Level.TRACE );
			var l2 = withLevel( SqlExecLogger.LOGGER_NAME, Logger.Level.TRACE );
			var l3 = withLevel( SecondLevelCacheLogger.LOGGER_NAME, Logger.Level.TRACE );) {
			final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
			statistics.clear();

			assertThat( statistics.getPrepareStatementCount(), is( 0L ) );

			final String hql = "select e from AggregateEntity e join fetch e.value1 join fetch e.value2";

			scope.inTransaction(
					session -> {
						resultsLogger.debug( "First query ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );

						final List<AggregateEntity> values = session.createQuery( hql, AggregateEntity.class )
								.setCacheable( true )
								.setCacheMode( CacheMode.NORMAL )
								.setCacheRegion( "fetch-region" )
								.list();

						verifyFetchResults( values );

						assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
					}
			);

			scope.inTransaction(
					session -> {
						resultsLogger.debug( "Second query ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );

						final List<AggregateEntity> values = session.createQuery( hql, AggregateEntity.class )
								.setCacheable( true )
								.setCacheMode( CacheMode.NORMAL )
								.setCacheRegion( "fetch-region" )
								.list();

						verifyFetchResults( values );

						assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
					}
			);
		}
	}

	private void verifyFetchResults(List<AggregateEntity> values) {
		assertThat( values.size(), is( 1 ) );
		final AggregateEntity rootEntity = values.get( 0 );
		assertThat( rootEntity.getValue1(), notNullValue() );
		assertTrue( Hibernate.isInitialized( rootEntity.getValue1() ) );
		assertThat( rootEntity.getValue1().getId(), is( 1 ) );
		assertThat( rootEntity.getValue1().getName(), is( "first" ) );
		assertThat( rootEntity.getValue2(), notNullValue() );
		assertTrue( Hibernate.isInitialized( rootEntity.getValue2() ) );
		assertThat( rootEntity.getValue2().getId(), is( 2 ) );
		assertThat( rootEntity.getValue2().getName(), is( "second" ) );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist(
						new AggregateEntity(
								1,
								"aggregate",
								new TestEntity( 1, "first" ),
								new TestEntity( 2, "second" )
						)
				)
		);
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
