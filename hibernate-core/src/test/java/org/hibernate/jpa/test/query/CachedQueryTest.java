/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.SharedCacheMode;
import javax.persistence.TypedQuery;

import org.hibernate.cfg.Environment;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9573" )
public class CachedQueryTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testCacheableQuery() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		for ( int i = 0 ; i < 10 ; i++ ) {
			Employee employee = new Employee( "John" + i, 20d + i);
			em.persist( employee );
		}
		em.getTransaction().commit();
		em.close();

		HibernateEntityManagerFactory hemf =  (HibernateEntityManagerFactory) entityManagerFactory();
		Statistics stats = hemf.getSessionFactory().getStatistics();

		assertEquals( 0, stats.getQueryCacheHitCount() );
		assertEquals( 0, stats.getQueryCacheMissCount() );
		assertEquals( 0, stats.getQueryCachePutCount() );
		assertEquals( 0, stats.getSecondLevelCacheHitCount() );
		assertEquals( 0, stats.getSecondLevelCacheMissCount() );
		assertEquals( 10, stats.getSecondLevelCachePutCount() );

		stats.clear();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();

		// First time the query is executed, query and results are cached.

		TypedQuery<Employee> query = em.createQuery( "select e from Employee e", Employee.class )
				.setHint( QueryHints.HINT_CACHEABLE, true );
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

		em.getTransaction().commit();
		em.close();

		stats.clear();

		// Second time the query is executed, list of entities are read from query cache and
		// the entities themselves are read from the entity cache.

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		query = em.createQuery( "select e from Employee e", Employee.class )
				.setHint( QueryHints.HINT_CACHEABLE, true );
		employees = query.getResultList();
		assertEquals( 10, employees.size() );
		assertEquals( 1, stats.getQueryCacheHitCount() );
		assertEquals( 0, stats.getQueryCacheMissCount() );
		assertEquals( 0, stats.getQueryCachePutCount() );
		// the first time the query executes, stats.getSecondLevelCacheHitCount() is 0 because the
		// entities are read from the query ResultSet (not from the entity cache).
		assertEquals( 10, stats.getSecondLevelCacheHitCount() );
		assertEquals( 0, stats.getSecondLevelCacheMissCount() );
		assertEquals( 0, stats.getSecondLevelCachePutCount() );

		em.getTransaction().commit();
		em.close();

		// NOTE: JPACache.evictAll() only evicts entity regions;
		//       it does not evict the collection regions or query cache region
		entityManagerFactory().getCache().evictAll();

		stats.clear();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		query = em.createQuery( "select e from Employee e", Employee.class )
				.setHint( QueryHints.HINT_CACHEABLE, true );
		employees = query.getResultList();
		assertEquals( 10, employees.size() );
		// query is still found in the cache
		assertEquals( 1, stats.getQueryCacheHitCount() );
		assertEquals( 0, stats.getQueryCacheMissCount() );
		assertEquals( 0, stats.getQueryCachePutCount() );
		// since entity regions were evicted, the 10 entities are not found, and are re-put afterQuery loading
		// as each entity ID is read from the query cache, Hibernate will look the entity up in the
		// cache and will not find it; that's why the "miss" and "put" counts are both 10.
		assertEquals( 0, stats.getSecondLevelCacheHitCount() );
		assertEquals( 10, stats.getSecondLevelCacheMissCount() );
		assertEquals( 10, stats.getSecondLevelCachePutCount() );

		em.getTransaction().commit();
		em.close();

		stats.clear();

		// this time call clear the entity regions and the query cache region
		em = getOrCreateEntityManager();

		em.getEntityManagerFactory().getCache().evictAll();
		em.unwrap( HibernateEntityManagerImplementor.class )
				.getFactory()
				.getSessionFactory()
				.getCache()
				.evictQueryRegions();

		em.getTransaction().begin();
		query = em.createQuery( "select e from Employee e", Employee.class )
				.setHint( QueryHints.HINT_CACHEABLE, true );
		employees = query.getResultList();
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
		em.close();
	}

	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.ALL );
		options.put( Environment.GENERATE_STATISTICS, "true" );
		options.put( Environment.USE_QUERY_CACHE, "true" );
		options.put( Environment.USE_SECOND_LEVEL_CACHE, "true" );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Employee.class
		};
	}

}
