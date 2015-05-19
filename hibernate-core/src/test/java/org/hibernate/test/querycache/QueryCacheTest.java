/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querycache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.criterion.Restrictions;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.transform.Transformers;

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
		sessionFactory().getStatistics().clear();

		final String queryString = "from Item i where i.name='widget'";

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

		Thread.sleep(200);

		s = openSession();
		t = s.beginTransaction();
		List result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 0 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 1 );
		assertEquals( s.getSessionFactory().getStatistics().getEntityFetchCount(), 0 );

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		assertEquals( result.size(), 1 );
		assertTrue( Hibernate.isInitialized( result.get(0) ) );
		i = (Item) result.get(0);
		i.setName("Widget");
		t.commit();
		s.close();

		assertEquals( qs.getCacheHitCount(), 2 );
		assertEquals( qs.getCacheMissCount(), 2 );
		assertEquals( s.getSessionFactory().getStatistics().getEntityFetchCount(), 0 );

		Thread.sleep(200);

		s = openSession();
		t = s.beginTransaction();
		result = s.createQuery( queryString ).setCacheable(true).list();
		i = (Item) s.get( Item.class, new Long(i.getId()) );

		s.delete(i);
		t.commit();
		s.close();

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

}

