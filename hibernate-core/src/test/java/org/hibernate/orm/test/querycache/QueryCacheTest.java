/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hibernate.Hibernate;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.transform.Transformers;
import org.hibernate.type.Type;

import org.hibernate.testing.jdbc.ConnectionProviderDelegate;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProviderImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 * @author Brett Meyer
 * @author Réda Housni Alaoui
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/querycache/Item.hbm.xml",
		annotatedClasses = {
				CompositeKey.class,
				EntityWithCompositeKey.class,
				StringCompositeKey.class,
				EntityWithStringCompositeKey.class
		},
		concurrencyStrategy = "nonstrict-read-write"
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_PREFIX, value = "foo"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = JdbcSettings.CONNECTION_PROVIDER, value = "org.hibernate.orm.test.querycache.QueryCacheTest$ProxyConnectionProvider")
		}
)
public class QueryCacheTest {

	private static final CompositeKey PK = new CompositeKey( 1, 2 );
	private static final ExecutorService executor = Executors.newFixedThreadPool( 4 );


	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "from java.lang.Object" ).list().forEach( session::remove )
		);
	}

	@AfterAll
	protected void shutDown() {
		executor.shutdown();
	}


	@Test
	@JiraKey("HHH-5426")
	public void testInvalidationFromBulkHQL(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictQueryRegions();
		scope.getSessionFactory().getStatistics().clear();

		scope.inTransaction(
				session -> {
					for ( int i = 0; i < 3; i++ ) {
						Item a = new Item();
						a.setName( "a" + i );
						a.setDescription( "a" + i );
						session.persist( a );
					}
				}
		);

		String queryString = "select count(*) from Item";
		scope.inTransaction(
				session -> {
					// this query will hit the database and create the cache
					Long result = (Long) session.createQuery( queryString ).setCacheable( true ).uniqueResult();
					assertEquals( 3, result.intValue() );
				}
		);

		scope.inTransaction(
				session ->
						session.createQuery( "delete from Item" ).executeUpdate()
		);

		scope.inTransaction(
				session -> {
					// and this one SHOULD not be served by the cache
					Number result2 = (Number) session.createQuery( queryString ).setCacheable( true ).uniqueResult();
					assertEquals( 0, result2.intValue() );
				}
		);
	}

	@Test
	@JiraKey("JBPAPP-4224")
	public void testHitCacheInSameSession(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictQueryRegions();
		scope.getSessionFactory().getStatistics().clear();

		List<Item> list = new ArrayList<>();
		scope.inSession(
				session -> {
					try {
						session.beginTransaction();
						for ( int i = 0; i < 3; i++ ) {
							Item a = new Item();
							a.setName( "a" + i );
							a.setDescription( "a" + i );
							list.add( a );
							session.persist( a );
						}
						session.getTransaction().commit();

//		s.close();
//		s=openSession();

						session.beginTransaction();
						String queryString = "from Item";
						// this query will hit the database and create the cache
						session.createQuery( queryString ).setCacheable( true ).list();
						session.getTransaction().commit();

						session.beginTransaction();
						//and this one SHOULD served by the cache
						session.createQuery( queryString ).setCacheable( true ).list();
						session.getTransaction().commit();
						QueryStatistics qs = session.getSessionFactory()
								.getStatistics()
								.getQueryStatistics( queryString );
						assertEquals( 1, qs.getCacheHitCount() );
						assertEquals( 1, qs.getCachePutCount() );
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
		scope.inTransaction(
				session -> {
					for ( Object obj : list ) {
						session.remove( obj );
					}
				}
		);
	}

	private static final String queryString = "from Item i where i.name='widget'";

	@Test
	public void testQueryCacheInvalidation(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory().getCache().evictQueryRegions();

		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		final String queryString = "from Item i where i.name='widget'";

		final QueryStatistics qs = statistics.getQueryStatistics( queryString );
		final EntityStatistics es = statistics.getEntityStatistics( Item.class.getName() );

		scope.inTransaction(
				session -> {
					session.createQuery( queryString ).setCacheable( true ).list();
					Item i = new Item();
					i.setName( "widget" );
					i.setDescription( "A really top-quality, full-featured widget." );
					session.persist( i );
				}
		);

		// hit -> 0
		// miss -> 1
		// put -> 1

		assertEquals( 1, es.getInsertCount() );
		assertEquals( 0, es.getUpdateCount() );

		assertEquals( 0, statistics.getQueryCacheHitCount() );
		assertEquals( 0, qs.getCacheHitCount() );

		assertEquals( 1, statistics.getQueryCacheMissCount() );
		assertEquals( 1, qs.getCacheMissCount() );

		assertEquals( 1, statistics.getQueryCachePutCount() );
		assertEquals( 1, qs.getCachePutCount() );

		assertEquals( 1, statistics.getQueryExecutionCount() );
		assertEquals( 1, qs.getExecutionCount() );

		assertEquals( 0, statistics.getEntityFetchCount() );


		scope.inTransaction(
				session -> {
					List<Item> result = session.createQuery( queryString ).setCacheable( true ).list();
					assertEquals( 1, result.size() );
				}
		);

		// hit -> 0
		// miss -> 2
		// put -> 2

		assertEquals( 1, es.getInsertCount() );
		assertEquals( 0, es.getUpdateCount() );

		assertEquals( 0, statistics.getQueryCacheHitCount() );
		assertEquals( 0, qs.getCacheHitCount() );

		assertEquals( 2, statistics.getQueryCacheMissCount() );
		assertEquals( 2, qs.getCacheMissCount() );

		assertEquals( 2, statistics.getQueryCachePutCount() );
		assertEquals( 2, qs.getCachePutCount() );

		assertEquals( 2, statistics.getQueryExecutionCount() );
		assertEquals( 2, qs.getExecutionCount() );

		assertEquals( 0, statistics.getEntityFetchCount() );

		scope.inTransaction(
				session -> {
					List result = session.createQuery( queryString ).setCacheable( true ).list();
					assertEquals( 1, result.size() );
				}
		);

		// hit -> 1
		// miss -> 2
		// put -> 2

		assertEquals( 1, es.getInsertCount() );
		assertEquals( 0, es.getUpdateCount() );

		assertEquals( 1, statistics.getQueryCacheHitCount() );
		assertEquals( 1, qs.getCacheHitCount() );

		assertEquals( 2, statistics.getQueryCacheMissCount() );
		assertEquals( 2, qs.getCacheMissCount() );

		assertEquals( 2, statistics.getQueryCachePutCount() );
		assertEquals( 2, qs.getCachePutCount() );

		assertEquals( 2, statistics.getQueryExecutionCount() );
		assertEquals( 2, qs.getExecutionCount() );

		assertEquals( 0, statistics.getEntityFetchCount() );

		assertEquals( 1, qs.getCacheHitCount() );
		assertEquals( 0, statistics.getEntityFetchCount() );

		Item item = scope.fromTransaction(
				session -> {
					List<Item> result = session.createQuery( queryString ).setCacheable( true ).list();
					assertEquals( 1, result.size() );
					Item i = result.get( 0 );
					assertTrue( Hibernate.isInitialized( i ) );
					assertTrue( session.contains( i ) );
					i.setName( "Widget" );
					session.flush();
					return i;
				}
		);

		// hit -> 2
		// miss -> 2
		// put -> 2
		//
		// + another invalidation

		assertEquals( 1, es.getInsertCount() );
		assertEquals( 1, es.getUpdateCount() );

		assertEquals( 2, statistics.getQueryCacheHitCount() );
		assertEquals( 2, qs.getCacheHitCount() );

		assertEquals( 2, statistics.getQueryCacheMissCount() );
		assertEquals( 2, qs.getCacheMissCount() );

		assertEquals( 2, statistics.getQueryCachePutCount() );
		assertEquals( 2, qs.getCachePutCount() );

		assertEquals( 2, statistics.getQueryExecutionCount() );
		assertEquals( 2, qs.getExecutionCount() );

		assertEquals( 0, statistics.getEntityFetchCount() );


		Thread.sleep( 200 );

		scope.inTransaction(
				session -> {
					session.createQuery( queryString ).setCacheable( true ).list();
					Item i = session.get( Item.class, item.getId() );

					session.remove( i );
				}
		);

		// hit -> 2
		// miss -> 3
		// put -> 3

		assertEquals( 1, es.getInsertCount() );
		assertEquals( 1, es.getUpdateCount() );

		assertEquals( 2, statistics.getQueryCacheHitCount() );
		assertEquals( 2, qs.getCacheHitCount() );

		assertEquals( 3, statistics.getQueryCacheMissCount() );
		assertEquals( 3, qs.getCacheMissCount() );

		assertEquals( 3, statistics.getQueryCachePutCount() );
		assertEquals( 3, qs.getCachePutCount() );

		assertEquals( 3, statistics.getQueryExecutionCount() );
		assertEquals( 3, qs.getExecutionCount() );

		assertEquals( 0, statistics.getEntityFetchCount() );
		assertEquals( 0, es.getFetchCount() );

		assertEquals( 2, qs.getCacheHitCount() );
		assertEquals( 3, qs.getCacheMissCount() );
		assertEquals( 3, qs.getCachePutCount() );
		assertEquals( 3, qs.getExecutionCount() );
		assertEquals( 0, es.getFetchCount() ); //check that it was being cached

	}

	@Test
	public void testComparison(SessionFactoryScope scope) {
		Item item = new Item();
		scope.inTransaction(
				session -> {
					item.setName( "widget" );
					item.setDescription( "A really top-quality, full-featured widget." );
					session.persist( item );
				}
		);

		scope.inTransaction(
				session -> {
					List result = session.createQuery( queryString ).list();
					assertEquals( 1, result.size() );
					Item i = session.get( Item.class, item.getId() );
					assertEquals( "widget", i.getName() );
					session.remove( i );
				}
		);
	}


	@Test
	public void testProjectionCache(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory().getCache().evictQueryRegions();
		scope.getSessionFactory().getStatistics().clear();

		final String queryString = "select i.description as desc from Item i where i.name='widget'";

		Item item = new Item();
		scope.inTransaction(
				session -> {
					session.createQuery( queryString ).setCacheable( true ).list();
					item.setName( "widget" );
					item.setDescription( "A really top-quality, full-featured widget." );
					session.persist( item );
				}
		);

		QueryStatistics qs = scope.getSessionFactory().getStatistics().getQueryStatistics( queryString );
		EntityStatistics es = scope.getSessionFactory().getStatistics().getEntityStatistics( Item.class.getName() );

		assertEquals( 0, qs.getCacheHitCount() );
		assertEquals( 1, qs.getCacheMissCount() );
		assertEquals( 1, qs.getCachePutCount() );

		Thread.sleep( 200 );

		scope.inTransaction(
				session -> {
					List result = session.createQuery( queryString ).setCacheable( true ).list();
					assertEquals( 1, result.size() );
					assertEquals( item.getDescription(), result.get( 0 ) );
				}
		);


		assertEquals( 0, qs.getCacheHitCount() );
		assertEquals( 2, qs.getCacheMissCount() );
		assertEquals( 2, qs.getCachePutCount() );

		scope.inTransaction(
				session -> {
					List result = session.createQuery( queryString ).setCacheable( true ).list();
					assertEquals( 1, result.size() );
					assertEquals( item.getDescription(), result.get( 0 ) );
				}
		);

		assertEquals( 1, qs.getCacheHitCount() );
		assertEquals( 2, qs.getCacheMissCount() );
		assertEquals( 2, qs.getCachePutCount() );

		scope.inTransaction(
				session -> {
					List result = session.createQuery( queryString )
							.setCacheable( true )
							.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP )
							.list();
					assertEquals( 1, result.size() );
					Map m = (Map) result.get( 0 );
					assertEquals( 1, m.size() );
					assertEquals( item.getDescription(), m.get( "desc" ) );
				}
		);

		assertEquals(
				2,
				qs.getCacheHitCount(),
				"hit count should go up since data is not transformed until after it is cached"
		);
		assertEquals( 2, qs.getCacheMissCount() );
		assertEquals( 2, qs.getCachePutCount() );

		scope.inTransaction(
				session -> {
					List result = session.createQuery( queryString )
							.setCacheable( true )
							.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP )
							.list();
					assertEquals( 1, result.size() );
					Map m = (Map) result.get( 0 );
					assertEquals( 1, m.size() );
					assertEquals( item.getDescription(), m.get( "desc" ) );
				}
		);

		assertEquals(
				3,
				qs.getCacheHitCount(),
				"hit count should go up since data is not transformed until after it is cachedr"
		);
		assertEquals( 2, qs.getCacheMissCount() );
		assertEquals( 2, qs.getCachePutCount() );

		scope.inTransaction(
				session -> {
					List result = session.createQuery( queryString ).setCacheable( true ).list();
					assertEquals( 1, result.size() );
					assertTrue( Hibernate.isInitialized( result.get( 0 ) ) );
					Item i = session.get( Item.class, item.getId() );
					i.setName( "widget" );
					i.setDescription( "A middle-quality widget." );
				}
		);

		assertEquals( 4, qs.getCacheHitCount() );
		assertEquals( 2, qs.getCacheMissCount() );
		assertEquals( 2, qs.getCachePutCount() );

		Thread.sleep( 200 );

		scope.inTransaction(
				session -> {
					List result = session.createQuery( queryString ).setCacheable( true ).list();
					assertEquals( 1, result.size() );
					Item i = session.get( Item.class, item.getId() );
					assertEquals( "A middle-quality widget.", result.get( 0 ) );

					assertEquals( 4, qs.getCacheHitCount() );
					assertEquals( 3, qs.getCacheMissCount() );
					assertEquals( 3, qs.getCachePutCount() );

					session.remove( i );
				}
		);

		assertEquals( 4, qs.getCacheHitCount() );
		assertEquals( 3, qs.getCacheMissCount() );
		assertEquals( 3, qs.getCachePutCount() );
		assertEquals( 3, qs.getExecutionCount() );
		assertEquals( 0, es.getFetchCount() ); //check that it was being cached
	}

	@Test
	@JiraKey("HHH-4459")
	public void testGetByCompositeId(SessionFactoryScope scope) {

		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						session.persist( new EntityWithCompositeKey( PK ) );
						Query query = session.createQuery( "FROM EntityWithCompositeKey e WHERE e.pk = :pk" );
						query.setCacheable( true );
						query.setParameter( "pk", PK );
						assertEquals( 1, query.list().size() );
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);

		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						EntityWithStringCompositeKey entity = new EntityWithStringCompositeKey();
						StringCompositeKey key = new StringCompositeKey();
						key.setAnalog( "foo1" );
						key.setDevice( "foo2" );
						key.setDeviceType( "foo3" );
						key.setSubstation( "foo4" );
						entity.setPk( key );
						session.persist( entity );
						CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
						CriteriaQuery<EntityWithStringCompositeKey> criteria = criteriaBuilder.createQuery(
								EntityWithStringCompositeKey.class );
						Root<EntityWithStringCompositeKey> root = criteria.from( EntityWithStringCompositeKey.class );
						criteria.where( criteriaBuilder.equal( root.get( "pk" ), key ) );
						session.createQuery( criteria ).setCacheable( true );

						assertEquals( 1, session.createQuery( criteria ).list().size() );
//		Criteria c = s.createCriteria(
//				EntityWithStringCompositeKey.class ).add( Restrictions.eq(
//						"pk", key ) );
//		c.setCacheable( true );
//		assertEquals( 1, c.list().size() );
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@JiraKey("HHH-3051")
	public void testScalarSQLQuery(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictQueryRegions();
		scope.getSessionFactory().getStatistics().clear();

		scope.inTransaction(
				session -> {
					Item item = new Item();
					item.setName( "fooName" );
					item.setDescription( "fooDescription" );
					session.persist( item );
				}
		);

		scope.inTransaction(
				session -> {
					// Note: StandardQueryCache#put handles single results and multiple results differently.  So, test both
					// 1 and 2+ scalars.

					String sqlQuery = "select name, description from Items";
					NativeQuery query = session.createNativeQuery( sqlQuery );
					query.setCacheable( true );
					query.addScalar( "name" );
					query.addScalar( "description" );
					Object[] result1 = (Object[]) query.uniqueResult();
					assertNotNull( result1 );
					assertEquals( 2, result1.length );
					assertEquals( "fooName", result1[0] );
					assertEquals( "fooDescription", result1[1] );

					sqlQuery = "select name from Items";
					query = session.createNativeQuery( sqlQuery );
					query.setCacheable( true );
					query.addScalar( "name" );
					String result2 = (String) query.uniqueResult();
					assertNotNull( result2 );
					assertEquals( "fooName", result2 );
				}
		);
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

//	@Test
//	@JiraKey("HHH-9962")
//	/* Test courtesy of Giambattista Bloisi */
//	public void testDelayedLoad(SessionFactoryScope scope) throws InterruptedException, ExecutionException {
//		DelayLoadOperations interceptor = new DelayLoadOperations();
//		final SessionBuilder sessionBuilder = scope.getSessionFactory().withOptions().interceptor( interceptor );
//		Item item1 = new Item();
//		item1.setName( "Item1" );
//		item1.setDescription( "Washington" );
//
//		try (Session s1 = sessionBuilder.openSession()) {
//			Transaction tx1 = s1.beginTransaction();
//			try {
//				s1.persist( item1 );
//				tx1.commit();
//			}
//			finally {
//				if ( tx1.isActive() ) {
//					tx1.rollback();
//				}
//			}
//		}
//
//		Item item2 = new Item();
//		item2.setName( "Item2" );
//		item2.setDescription( "Chicago" );
//		try (Session s2 = sessionBuilder.openSession()) {
//			Transaction tx2 = s2.beginTransaction();
//			try {
//				s2.persist( item2 );
//				tx2.commit();
//			}
//			finally {
//				if ( tx2.isActive() ) {
//					tx2.rollback();
//				}
//			}
//		}
//
//		interceptor.blockOnLoad();
//
//		Future<Item> fetchedItem = executor.submit( () -> findByDescription( sessionBuilder, "Washington" ) );
//
//		// wait for the onLoad listener to be called
//		interceptor.waitOnLoad();
//
//		try (Session s3 = sessionBuilder.openSession()) {
//			Transaction tx3 = s3.beginTransaction();
//			try {
//				item1.setDescription( "New York" );
//				item2.setDescription( "Washington" );
//				s3.update( item1 );
//				s3.update( item2 );
//				tx3.commit();
//			}
//			finally {
//				if ( tx3.isActive() ) {
//					tx3.rollback();
//				}
//			}
//		}
//
//		interceptor.unblockOnLoad();
//
//		// the concurrent query was executed before the data was amended so
//		// let's expect "Item1" to be returned as living in Washington
//		Item fetched = fetchedItem.get();
//		assertEquals( "Item1", fetched.getName() );
//
//		// Query again: now "Item2" is expected to live in Washington
//		fetched = findByDescription( sessionBuilder, "Washington" );
//		assertEquals( "Item2", fetched.getName() );
//	}

	@Test
	@JiraKey("HHH-18371")
	public void testConnectionFailure(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictQueryRegions();
		scope.getSessionFactory().getStatistics().clear();

		Item item = new Item();
		scope.inTransaction(
				session -> {
					item.setName( "widget" );
					item.setDescription( "A really top-quality, full-featured widget." );
					session.persist( item );
				}
		);

		scope.inTransaction(
				session -> ProxyConnectionProvider.runWithConnectionRetrievalFailure( new SQLException(
						"Too many connections" ), () -> {
					try {
						session.createQuery( queryString ).setCacheable( true ).list();
						fail( "Failure expected" );
					}
					catch (RuntimeException e) {
						assertTrue( e.getMessage().contains( "Too many connections" ) );
					}
				} )
		);

		scope.inTransaction(
				session -> {
					List result = session.createQuery( queryString ).setCacheable( true ).list();
					assertEquals( 1, result.size() );
					Item i = session.get( Item.class, item.getId() );
					assertEquals( "widget", i.getName() );
					session.remove( i );
				}
		);
	}

	protected Item findByDescription(SessionBuilder sessionBuilder, final String description) {
		try (Session s = sessionBuilder.openSession()) {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Item> criteria = criteriaBuilder.createQuery( Item.class );
			Root<Item> root = criteria.from( Item.class );
			criteria.where( criteriaBuilder.equal( root.get( "description" ), description ) );

			return s.createQuery( criteria ).setCacheable( true ).setReadOnly( true ).uniqueResult();
//			return (Item) s.createCriteria(Item.class)
//               .setCacheable(true)
//               .setReadOnly(true)
//               .add(Restrictions.eq("description", description))
//               .uniqueResult();

		}
	}

	public class DelayLoadOperations implements Interceptor {

		private volatile CountDownLatch blockLatch;
		private volatile CountDownLatch waitLatch;

		@Override
		public boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types){
			onLoad();
			return true;
		}

		private void onLoad() {
			// Synchronize load and update activities
			try {
				if ( waitLatch != null ) {
					waitLatch.countDown();
				}
				if ( blockLatch != null ) {
					blockLatch.await();
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException( e );
			}
		}



		public void blockOnLoad() {
			blockLatch = new CountDownLatch( 1 );
			waitLatch = new CountDownLatch( 1 );
		}

		public void waitOnLoad() throws InterruptedException {
			waitLatch.await();
		}

		public void unblockOnLoad() {
			if ( blockLatch != null ) {
				blockLatch.countDown();
			}
		}
	}

	public static class ProxyConnectionProvider extends ConnectionProviderDelegate {

		private static final ThreadLocal<SQLException> CONNECTION_RETRIEVAL_EXCEPTION_TO_THROW = new ThreadLocal<>();

		public ProxyConnectionProvider() {
			setConnectionProvider( SharedDriverManagerConnectionProviderImpl.getInstance() );
		}

		static void runWithConnectionRetrievalFailure(SQLException exceptionToThrow, Runnable runnable) {
			CONNECTION_RETRIEVAL_EXCEPTION_TO_THROW.set( exceptionToThrow );
			try {
				runnable.run();
			}
			finally {
				CONNECTION_RETRIEVAL_EXCEPTION_TO_THROW.remove();
			}
		}

		@Override
		public Connection getConnection() throws SQLException {
			SQLException exceptionToSend = CONNECTION_RETRIEVAL_EXCEPTION_TO_THROW.get();
			if ( exceptionToSend != null ) {
				throw exceptionToSend;
			}
			return super.getConnection();
		}
	}
}
