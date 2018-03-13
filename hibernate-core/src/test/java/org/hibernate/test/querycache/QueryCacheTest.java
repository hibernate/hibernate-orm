/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querycache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hibernate.Criteria;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.criterion.Restrictions;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.transform.Transformers;
import org.hibernate.type.Type;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 * @author Brett Meyer
 */
public class QueryCacheTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final CompositeKey PK = new CompositeKey(1, 2);
	private static final ExecutorService executor = Executors.newFixedThreadPool(4);
	
	@Override
	public String[] getMappings() {
		return new String[] { "querycache/Item.hbm.xml" };
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { 
				CompositeKey.class,
				EntityWithCompositeKey.class,
				StringCompositeKey.class,
				EntityWithStringCompositeKey.class				
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.USE_QUERY_CACHE, "true" );
		settings.put( AvailableSettings.CACHE_REGION_PREFIX, "foo" );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void shutDown() {
		super.shutDown();
		executor.shutdown();
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5426" )
	public void testInvalidationFromBulkHQL() {
		sessionFactory().getCache().evictQueryRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		List list = new ArrayList();
		s.beginTransaction();
		for (int i = 0; i < 3; i++) {
			Item a = new Item();
			a.setName("a" + i);
			a.setDescription("a" + i);
			list.add(a);
			s.persist(a);
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		String queryString = "select count(*) from Item";
		// this query will hit the database and create the cache
		Long result = (Long) s.createQuery(queryString).setCacheable(true).uniqueResult();
		assertEquals(3, result.intValue());
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		String updateString = "delete from Item";
		s.createQuery(updateString).executeUpdate();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		// and this one SHOULD not be served by the cache
		Number result2 = (Number) s.createQuery(queryString).setCacheable(true).uniqueResult();
		assertEquals(0, result2.intValue());
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "JBPAPP-4224" )
	public void testHitCacheInSameSession() {
		sessionFactory().getCache().evictQueryRegions();
		sessionFactory().getStatistics().clear();
		Session s = openSession();
		List list = new ArrayList();
		s.beginTransaction();
		for ( int i = 0; i < 3; i++ ) {
			Item a = new Item();
			a.setName( "a" + i );
			a.setDescription( "a" + i );
			list.add( a );
			s.persist( a );
		}
		s.getTransaction().commit();

//		s.close();
//		s=openSession();

		s.beginTransaction();
		String queryString = "from Item";
		// this query will hit the database and create the cache
		s.createQuery( queryString ).setCacheable( true ).list();
		s.getTransaction().commit();

		s.beginTransaction();
		//and this one SHOULD served by the cache
		s.createQuery( queryString ).setCacheable( true ).list();
		s.getTransaction().commit();
		QueryStatistics qs = s.getSessionFactory().getStatistics().getQueryStatistics( queryString );
		assertEquals( 1, qs.getCacheHitCount() );
		assertEquals( 1, qs.getCachePutCount() );
		s.close();
		s = openSession();
		s.beginTransaction();
		for(Object obj:list){
			s.delete( obj );
		}
		s.getTransaction().commit();
		s.close();

	}

	private static final String queryString = "from Item i where i.name='widget'";

	@Test
	public void testQueryCacheInvalidation() throws Exception {
		sessionFactory().getCache().evictQueryRegions();

		final StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();

		final String queryString = "from Item i where i.name='widget'";

		final QueryStatistics qs = statistics.getQueryStatistics( queryString );
		final EntityStatistics es = statistics.getEntityStatistics( Item.class.getName() );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery( queryString ).setCacheable(true).list();
		Item i = new Item();
		i.setName("widget");
		i.setDescription("A really top-quality, full-featured widget.");
		s.save(i);
		t.commit();
		s.close();

		// hit -> 0
		// miss -> 1
		// put -> 1

		assertEquals( es.getInsertCount(), 1 );
		assertEquals( es.getUpdateCount(), 0 );

		assertEquals( statistics.getQueryCacheHitCount(), 0 );
		assertEquals( qs.getCacheHitCount(), 0 );

		assertEquals( statistics.getQueryCacheMissCount(), 1 );
		assertEquals( qs.getCacheMissCount(), 1);

		assertEquals( statistics.getQueryCachePutCount(), 1 );
		assertEquals( qs.getCachePutCount(), 1);

		assertEquals( statistics.getQueryExecutionCount(), 1 );
		assertEquals( qs.getExecutionCount(), 1 );

		assertEquals( statistics.getEntityFetchCount(), 0 );


		Thread.sleep(200);

		s = openSession();
		t = s.beginTransaction();
		List result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		t.commit();
		s.close();

		// hit -> 0
		// miss -> 2
		// put -> 2

		assertEquals( es.getInsertCount(), 1 );
		assertEquals( es.getUpdateCount(), 0 );

		assertEquals( statistics.getQueryCacheHitCount(), 0 );
		assertEquals( qs.getCacheHitCount(), 0 );

		assertEquals( statistics.getQueryCacheMissCount(), 2 );
		assertEquals( qs.getCacheMissCount(), 2 );

		assertEquals( statistics.getQueryCachePutCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		assertEquals( statistics.getQueryExecutionCount(), 2 );
		assertEquals( qs.getExecutionCount(), 2 );

		assertEquals( statistics.getEntityFetchCount(), 0 );


		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		t.commit();
		s.close();

		// hit -> 1
		// miss -> 2
		// put -> 2

		assertEquals( es.getInsertCount(), 1 );
		assertEquals( es.getUpdateCount(), 0 );

		assertEquals( statistics.getQueryCacheHitCount(), 1 );
		assertEquals( qs.getCacheHitCount(), 1);

		assertEquals( statistics.getQueryCacheMissCount(), 2 );
		assertEquals( qs.getCacheMissCount(), 2 );

		assertEquals( statistics.getQueryCachePutCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		assertEquals( statistics.getQueryExecutionCount(), 2 );
		assertEquals( qs.getExecutionCount(), 2 );

		assertEquals( statistics.getEntityFetchCount(), 0 );


		assertEquals( qs.getCacheHitCount(), 1 );
		assertEquals( statistics.getEntityFetchCount(), 0 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		i = (Item) result.get(0);
		assertTrue( Hibernate.isInitialized( i ) );
		assertTrue( s.contains( i ) );
		i.setName("Widget");
		s.flush();
		t.commit();
		s.close();

		// hit -> 2
		// miss -> 2
		// put -> 2
		//
		// + another invalidation

		assertEquals( es.getInsertCount(), 1 );
		assertEquals( es.getUpdateCount(), 1 );

		assertEquals( statistics.getQueryCacheHitCount(), 2 );
		assertEquals( qs.getCacheHitCount(), 2 );

		assertEquals( statistics.getQueryCacheMissCount(), 2 );
		assertEquals( qs.getCacheMissCount(), 2 );

		assertEquals( statistics.getQueryCachePutCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		assertEquals( statistics.getQueryExecutionCount(), 2 );
		assertEquals( qs.getExecutionCount(), 2 );

		assertEquals( statistics.getEntityFetchCount(), 0 );


		Thread.sleep(200);

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		i = (Item) s.get( Item.class, new Long(i.getId()) );

		s.delete(i);
		t.commit();
		s.close();

		// hit -> 2
		// miss -> 3
		// put -> 3


		assertEquals( es.getInsertCount(), 1 );
		assertEquals( es.getUpdateCount(), 1 );

		assertEquals( statistics.getQueryCacheHitCount(), 2 );
		assertEquals( qs.getCacheHitCount(), 2 );

		assertEquals( statistics.getQueryCacheMissCount(), 3 );
		assertEquals( qs.getCacheMissCount(), 3 );

		assertEquals( statistics.getQueryCachePutCount(), 3);
		assertEquals( qs.getCachePutCount(), 3 );

		assertEquals( statistics.getQueryExecutionCount(), 3);
		assertEquals( qs.getExecutionCount(), 3 );

		assertEquals( statistics.getEntityFetchCount(), 0 );
		assertEquals( es.getFetchCount(), 0 );


		assertEquals( qs.getCacheHitCount(), 2 );
		assertEquals( qs.getCacheMissCount(), 3 );
		assertEquals( qs.getCachePutCount(), 3 );
		assertEquals( qs.getExecutionCount(), 3 );
		assertEquals( es.getFetchCount(), 0 ); //check that it was being cached

	}

	@Test
	@RequiresDialectFeature(
			value = DialectChecks.CaseSensitiveCheck.class,
			comment = "i.name='widget' should not match on case sensitive database."
	)
	public void testCaseInsensitiveComparison() {
		Session s = openSession();
		s.beginTransaction();
		Item i = new Item();
		i.setName( "Widget" );
		i.setDescription( "A really top-quality, full-featured widget." );
		s.save( i );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List result = s.createQuery( queryString ).list();
		assertEquals(1, result.size());
		i = (Item) s.get( Item.class, new Long(i.getId()) );
		assertEquals( i.getName(), "Widget" );
		s.delete(i);
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testQueryCacheFetch() throws Exception {
		sessionFactory().getCache().evictQueryRegions();
		sessionFactory().getStatistics().clear();

		// persist our 2 items.  This saves them to the db, but also into the second level entity cache region
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Item i = new Item();
		i.setName("widget");
		i.setDescription("A really top-quality, full-featured widget.");
		Item i2 = new Item();
		i2.setName("other widget");
		i2.setDescription("Another decent widget.");
		s.persist(i);
		s.persist(i2);
		t.commit();
		s.close();

		final String queryString = "from Item i where i.name like '%widget'";

		QueryStatistics qs = s.getSessionFactory().getStatistics().getQueryStatistics( queryString );

		Thread.sleep(200);

		// perform the cacheable query.  this will execute the query (no query cache hit), but the Items will be
		// found in second level entity cache region
		s = openSession();
		t = s.beginTransaction();
		List result = s.createQuery( queryString ).setCacheable( true ).list();
		assertEquals( result.size(), 2 );
		t.commit();
		s.close();
		assertEquals( qs.getCacheHitCount(), 0 );
		assertEquals( s.getSessionFactory().getStatistics().getEntityFetchCount(), 0 );


		// evict the Items from the second level entity cache region
		sessionFactory().getCache().evictEntityRegion( Item.class );

		// now, perform the cacheable query again.  this time we should not execute the query (query cache hit).
		// However, the Items will not be found in second level entity cache region this time (we evicted them above)
		// nor are they in associated with the session.
		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 2 );
		assertTrue( Hibernate.isInitialized( result.get(0) ) );
		assertTrue( Hibernate.isInitialized( result.get(1) ) );
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 1 );
		assertEquals( s.getSessionFactory().getStatistics().getEntityFetchCount(), 1 );

		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete Item").executeUpdate();
		t.commit();
		s.close();

	}

	@Test
	public void testProjectionCache() throws Exception {
		sessionFactory().getCache().evictQueryRegions();
        sessionFactory().getStatistics().clear();

		final String queryString = "select i.description as desc from Item i where i.name='widget'";

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery( queryString ).setCacheable(true).list();
		Item i = new Item();
		i.setName("widget");
		i.setDescription("A really top-quality, full-featured widget.");
		s.save(i);
		t.commit();
		s.close();

        QueryStatistics qs = s.getSessionFactory().getStatistics().getQueryStatistics( queryString );
		EntityStatistics es = s.getSessionFactory().getStatistics().getEntityStatistics( Item.class.getName() );

		assertEquals( qs.getCacheHitCount(), 0 );
		assertEquals( qs.getCacheMissCount(), 1 );
		assertEquals( qs.getCachePutCount(), 1 );

		Thread.sleep(200);

		s = openSession();
		t = s.beginTransaction();
		List result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		assertEquals( i.getDescription(), ( result.get( 0 ) ) );
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 0 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		assertEquals( i.getDescription(), result.get( 0 ) );
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 1 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
		assertEquals( result.size(), 1 );
		Map m = (Map) result.get(0);
		assertEquals( 1, m.size() );
		assertEquals( i.getDescription(), m.get( "desc" ) );
		t.commit();
		s.close();

		assertEquals( "hit count should go up since data is not transformed until after it is cached", qs.getCacheHitCount(), 2 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
		assertEquals( result.size(), 1 );
		m = (Map) result.get(0);
		assertEquals(1, m.size());
		assertEquals( i.getDescription(), m.get( "desc" ) );
		t.commit();
		s.close();

		assertEquals( "hit count should go up since data is not transformed until after it is cachedr", qs.getCacheHitCount(), 3 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		assertTrue( Hibernate.isInitialized( result.get(0) ) );
		i = (Item) s.get( Item.class, new Long(i.getId()) );
        i.setName("widget");
		i.setDescription("A middle-quality widget.");
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 4 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( qs.getCachePutCount(), 2 );

		Thread.sleep(200);

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		i = (Item) s.get( Item.class, new Long(i.getId()) );
		assertEquals( result.get(0), "A middle-quality widget." );

		assertEquals( qs.getCacheHitCount(), 4 );
		assertEquals( qs.getCacheMissCount(), 3 );
		assertEquals( qs.getCachePutCount(), 3 );

		s.delete(i);
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 4 );
		assertEquals( qs.getCacheMissCount(), 3 );
		assertEquals( qs.getCachePutCount(), 3 );
		assertEquals( qs.getExecutionCount(), 3 );
		assertEquals( es.getFetchCount(), 0 ); //check that it was being cached
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-4459" )
	public void testGetByCompositeId() {
		Session s = openSession();
		s.beginTransaction();
		s.persist( new EntityWithCompositeKey( PK ) );
		Query query = s.createQuery( "FROM EntityWithCompositeKey e WHERE e.pk = :pk" );
		query.setCacheable( true );
		query.setParameter( "pk", PK );
		assertEquals(1, query.list().size( ));
		s.getTransaction().rollback();
		s.close();
		
		s = openSession();
		s.beginTransaction();
		EntityWithStringCompositeKey entity = new EntityWithStringCompositeKey();
		StringCompositeKey key = new StringCompositeKey();
		key.setAnalog( "foo1" );
		key.setDevice( "foo2" );
		key.setDeviceType( "foo3" );
		key.setSubstation( "foo4" );
		entity.setPk( key );
		s.persist( entity );
		Criteria c = s.createCriteria(
				EntityWithStringCompositeKey.class ).add( Restrictions.eq( 
						"pk", key ) );
		c.setCacheable( true );
		assertEquals( 1, c.list().size() );
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3051" )
	public void testScalarSQLQuery() {
		sessionFactory().getCache().evictQueryRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		s.beginTransaction();
		Item item = new Item();
		item.setName("fooName");
		item.setDescription("fooDescription");
		s.persist(item);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		
		// Note: StandardQueryCache#put handles single results and multiple results differently.  So, test both
		// 1 and 2+ scalars.
		
        String sqlQuery = "select name, description from Items";
        SQLQuery query = s.createSQLQuery(sqlQuery);
        query.setCacheable(true);
        query.addScalar("name");
        query.addScalar("description");
        Object[] result1 = (Object[]) query.uniqueResult();
        assertNotNull( result1 );
        assertEquals( result1.length, 2 );
        assertEquals( result1[0], "fooName" );
        assertEquals( result1[1], "fooDescription" );
		
        sqlQuery = "select name from Items";
        query = s.createSQLQuery(sqlQuery);
        query.setCacheable(true);
        query.addScalar("name");
        String result2 = (String) query.uniqueResult();
        assertNotNull( result2 );
        assertEquals( result2, "fooName" );
        
        s.getTransaction().commit();
        s.close();
	}

//	@Test
//	public void testGetByCompositeIdNoCache() {
//		Query query = em.createQuery("FROM EntityWithCompositeKey e WHERE e.pk = :pk");
//		query.setParameter("pk", PK);
//		assertEquals(1, query.getResultList().size());
//	}
//
//	@Test
//	public void testGetByEntityIself() {
//		Query query = em.createQuery("FROM EntityWithCompositeKey e WHERE e = :ent");
//		query.setParameter("ent", new EntityWithCompositeKey(PK));
//		assertEquals(1, query.getResultList().size());
//	}

	@Test
	@TestForIssue(jiraKey = "HHH-9962")
	/* Test courtesy of Giambattista Bloisi */
	public void testDelayedLoad() throws InterruptedException, ExecutionException {
		DelayLoadOperations interceptor = new DelayLoadOperations();
		final SessionBuilder sessionBuilder = sessionFactory().withOptions().interceptor(interceptor);
		Item item1 = new Item();
		item1.setName("Item1");
		item1.setDescription("Washington");
		Session s1 = sessionBuilder.openSession();
		Transaction tx1 = s1.beginTransaction();
		s1.persist(item1);
		tx1.commit();
		s1.close();

		Item item2 = new Item();
		item2.setName("Item2");
		item2.setDescription("Chicago");
		Session s2 = sessionBuilder.openSession();
		Transaction tx2 = s2.beginTransaction();
		s2.persist(item2);
		tx2.commit();
		s2.close();

		interceptor.blockOnLoad();

		Future<Item> fetchedItem = executor.submit(new Callable<Item>() {
			public Item call() throws Exception {
				return findByDescription(sessionBuilder, "Washington");
			}
		});

		// wait for the onLoad listener to be called
		interceptor.waitOnLoad();

		Session s3 = sessionBuilder.openSession();
		Transaction tx3 = s3.beginTransaction();
		item1.setDescription("New York");
		item2.setDescription("Washington");
		s3.update(item1);
		s3.update(item2);
		tx3.commit();
		s3.close();

		interceptor.unblockOnLoad();

		// the concurrent query was executed before the data was amended so
		// let's expect "Item1" to be returned as living in Washington
		Item fetched = fetchedItem.get();
		assertEquals("Item1", fetched.getName());

		// Query again: now "Item2" is expected to live in Washington
		fetched = findByDescription(sessionBuilder, "Washington");
		assertEquals("Item2", fetched.getName());
	}

	protected Item findByDescription(SessionBuilder sessionBuilder, final String description) {
		Session s = sessionBuilder.openSession();
		try {
         return (Item) s.createCriteria(Item.class)
               .setCacheable(true)
               .setReadOnly(true)
               .add(Restrictions.eq("description", description))
               .uniqueResult();

      } finally {
         s.close();
      }
	}

	public class DelayLoadOperations extends EmptyInterceptor {

		private volatile CountDownLatch blockLatch;
		private volatile CountDownLatch waitLatch;

		@Override
		public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
			// Synchronize load and update activities
			try {
				if (waitLatch != null) {
					waitLatch.countDown();
					waitLatch = null;
				}
				if (blockLatch != null) {
					blockLatch.await();
					blockLatch = null;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
			return true;
		}

		public void blockOnLoad() {
			blockLatch = new CountDownLatch(1);
			waitLatch = new CountDownLatch(1);
		}

		public void waitOnLoad() throws InterruptedException {
			waitLatch.await();
		}

		public void unblockOnLoad() {
			if (blockLatch != null) {
				blockLatch.countDown();
			}
		}
	}
}

