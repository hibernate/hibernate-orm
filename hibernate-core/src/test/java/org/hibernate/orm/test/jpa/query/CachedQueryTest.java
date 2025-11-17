/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.TypedQuery;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@JiraKey(value = "HHH-9573")
@Jpa(
		annotatedClasses = Employee.class,
		generateStatistics = true,
		properties = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		},
		settingProviders = @SettingProvider(settingName = AvailableSettings.JAKARTA_SHARED_CACHE_MODE, provider = CachedQueryTest.SharedCacheModeProvider.class)
)
public class CachedQueryTest {

	public final static String HQL = "select e from Employee e";

	public static class SharedCacheModeProvider implements SettingProvider.Provider<SharedCacheMode> {
		@Override
		public SharedCacheMode getSetting() {
			return SharedCacheMode.ALL;
		}
	}

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					for ( int i = 0; i < 10; i++ ) {
						em.persist( new Employee( "John" + i, 20d + i ) );
					}
				}
		);
		Statistics stats = getStatistics( scope );

		assertEquals( 0, stats.getQueryCacheHitCount() );
		assertEquals( 0, stats.getQueryCacheMissCount() );
		assertEquals( 0, stats.getQueryCachePutCount() );
		assertEquals( 0, stats.getSecondLevelCacheHitCount() );
		assertEquals( 0, stats.getSecondLevelCacheMissCount() );
		assertEquals( 10, stats.getSecondLevelCachePutCount() );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCacheableQuery(EntityManagerFactoryScope scope) {

		Statistics stats = getStatistics( scope );
		stats.clear();

		// First time the query is executed, query and results are cached.
		scope.inTransaction(
				em -> {
					List<Employee> employees = getEmployees( em );

					assertThatAnSQLQueryHasBeenExecuted( stats );

					assertEquals( 0, stats.getQueryCacheHitCount() );
					assertEquals( 1, stats.getQueryCacheMissCount() );
					assertEquals( 1, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );
				}
		);

		stats.clear();

		// Second time the query is executed, list of entities are read from query cache

		scope.inTransaction(
				em -> {
					List<Employee> employees = getEmployees( em );

					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );
				}
		);

		// NOTE: JPACache.evictAll() only evicts entity regions;
		//       it does not evict the collection regions or query cache region
		scope.getEntityManagerFactory().getCache().evictAll();

		stats.clear();

		scope.inTransaction(
				em -> {
					List<Employee> employees = getEmployees( em );

					// query is still found in the cache
					assertThatNoSQLQueryHasBeenExecuted( stats );

					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );
				}
		);


		stats.clear();

		// this time call clear the entity regions and the query cache region
		scope.inTransaction(
				em -> {
					em.getEntityManagerFactory().getCache().evictAll();
					em.unwrap( SessionImplementor.class )
							.getFactory()
							.getCache()
							.evictQueryRegions();

					List<Employee> employees = getEmployees( em );

					// query is no longer found in the cache
					assertThatAnSQLQueryHasBeenExecuted( stats );

					assertEquals( 0, stats.getQueryCacheHitCount() );
					assertEquals( 1, stats.getQueryCacheMissCount() );
					assertEquals( 1, stats.getQueryCachePutCount() );

					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 10, stats.getSecondLevelCachePutCount() );
				}
		);

	}

	private static Statistics getStatistics(EntityManagerFactoryScope scope) {
		return ( (SessionFactoryImplementor) scope.getEntityManagerFactory() ).getStatistics();
	}

	private static List<Employee> getEmployees(EntityManager em) {
		TypedQuery<Employee> query = em.createQuery(
						HQL,
						Employee.class
				)
				.setHint( HINT_CACHEABLE, true );
		List<Employee> employees = query.getResultList();
		assertEquals( 10, employees.size() );
		return employees;
	}

	private static void assertThatAnSQLQueryHasBeenExecuted(Statistics stats) {
		assertEquals( 1, stats.getQueryStatistics( HQL ).getExecutionCount() );
	}

	private static void assertThatNoSQLQueryHasBeenExecuted(Statistics stats) {
		assertEquals( 0, stats.getQueryStatistics( HQL ).getExecutionCount() );
	}

}
