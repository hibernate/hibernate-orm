/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.query;

import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.TypedQuery;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-9573")
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

	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ALL );
	}

	public static class SharedCacheModeProvider implements SettingProvider.Provider<SharedCacheMode> {
		@Override
		public SharedCacheMode getSetting() {
			return SharedCacheMode.ALL;
		}
	}


	@Test
	// todo (6.0): implement shallow query cache structure
	@NotImplementedYet(strict = false, reason = "Different query cache structure")
	public void testCacheableQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					for ( int i = 0; i < 10; i++ ) {
						Employee employee = new Employee( "John" + i, 20d + i );
						em.persist( employee );
					}
				}
		);

		SessionFactoryImplementor hemf = (SessionFactoryImplementor) scope.getEntityManagerFactory();
		Statistics stats = hemf.getStatistics();

		assertEquals( 0, stats.getQueryCacheHitCount() );
		assertEquals( 0, stats.getQueryCacheMissCount() );
		assertEquals( 0, stats.getQueryCachePutCount() );
		assertEquals( 0, stats.getSecondLevelCacheHitCount() );
		assertEquals( 0, stats.getSecondLevelCacheMissCount() );
		assertEquals( 10, stats.getSecondLevelCachePutCount() );

		stats.clear();

		// First time the query is executed, query and results are cached.

		scope.inTransaction(
				em -> {
					TypedQuery<Employee> query = em.createQuery( "select e from Employee e", Employee.class )
							.setHint( HINT_CACHEABLE, true );
					List<Employee> employees = query.getResultList();
					assertEquals( 10, employees.size() );
					assertEquals( 0, stats.getQueryCacheHitCount() );
					assertEquals( 1, stats.getQueryCacheMissCount() );
					assertEquals( 1, stats.getQueryCachePutCount() );
					// the first time the query executes, stats.getSecondLevelCacheHitCount() is 0 because the
					// entities are read from the query ResultSet (not from the entity cache).
					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 0, stats.getSecondLevelCacheMissCount() );
					assertEquals( 0, stats.getSecondLevelCachePutCount() );
				}
		);


		stats.clear();

		// Second time the query is executed, list of entities are read from query cache and
		// the entities themselves are read from the entity cache.

		scope.inTransaction(
				em -> {
					TypedQuery<Employee> query = em.createQuery( "select e from Employee e", Employee.class )
							.setHint( HINT_CACHEABLE, true );
					List<Employee> employees = query.getResultList();
					assertEquals( 10, employees.size() );
					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );
					// the first time the query executes, stats.getSecondLevelCacheHitCount() is 0 because the
					// entities are read from the query ResultSet (not from the entity cache).
					assertEquals( 10, stats.getSecondLevelCacheHitCount() );
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
					TypedQuery<Employee> query = em.createQuery( "select e from Employee e", Employee.class )
							.setHint( HINT_CACHEABLE, true );
					List<Employee> employees = query.getResultList();
					assertEquals( 10, employees.size() );
					// query is still found in the cache
					assertEquals( 1, stats.getQueryCacheHitCount() );
					assertEquals( 0, stats.getQueryCacheMissCount() );
					assertEquals( 0, stats.getQueryCachePutCount() );
					// since entity regions were evicted, the 10 entities are not found, and are re-put after loading
					// as each entity ID is read from the query cache, Hibernate will look the entity up in the
					// cache and will not find it; that's why the "miss" and "put" counts are both 10.
					assertEquals( 0, stats.getSecondLevelCacheHitCount() );
					assertEquals( 10, stats.getSecondLevelCacheMissCount() );
					assertEquals( 10, stats.getSecondLevelCachePutCount() );
				}
		);


		stats.clear();

		// this time call clear the entity regions and the query cache region
		scope.inEntityManager(
				em -> {
					em.getEntityManagerFactory().getCache().evictAll();
					em.unwrap( SessionImplementor.class )
							.getFactory()
							.getCache()
							.evictQueryRegions();

					em.getTransaction().begin();
					try {
						TypedQuery<Employee> query = em.createQuery( "select e from Employee e", Employee.class )
								.setHint( HINT_CACHEABLE, true );
						List<Employee> employees = query.getResultList();
						assertEquals( 10, employees.size() );
						// query is no longer found in the cache
						assertEquals( 0, stats.getQueryCacheHitCount() );
						assertEquals( 1, stats.getQueryCacheMissCount() );
						assertEquals( 1, stats.getQueryCachePutCount() );
						// stats.getSecondLevelCacheHitCount() is 0 because the
						// entities are read from the query ResultSet (not from the entity cache).
						assertEquals( 0, stats.getSecondLevelCacheHitCount() );
						assertEquals( 0, stats.getSecondLevelCacheMissCount() );
						assertEquals( 10, stats.getSecondLevelCachePutCount() );

						em.createQuery( "delete from Employee" ).executeUpdate();
						em.getTransaction().commit();
					}
					finally {
						if ( em.getTransaction().isActive() ) {
							em.getTransaction().rollback();
						}
					}
				}
		);

	}

}
