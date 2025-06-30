/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;

import org.hibernate.testing.jta.JtaAwareConnectionProviderImpl;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Gavin King
 */
@SkipForDialect(dialectClass = SQLServerDialect.class, matchSubTypes = true)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/tm/Item.hbm.xml",
		concurrencyStrategy = "transactional"
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.AUTO_CLOSE_SESSION, value = "true"),
				@Setting(name = AvailableSettings.FLUSH_BEFORE_COMPLETION, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_PREFIX, value = ""),
				@Setting(name = AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, value = "true"),
				@Setting(name = "javax.persistence.transactionType", value = "JTA"),

		},
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.JTA_PLATFORM,
						provider = CMTTest.JtaPlatfomProvider.class
				),
				@SettingProvider(
						settingName = AvailableSettings.CONNECTION_PROVIDER,
						provider = CMTTest.ConnectionProvider.class
				),
				@SettingProvider(
						settingName = AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY,
						provider = CMTTest.TransactionCoordinatorStrategyProvider.class
				),
				@SettingProvider(
						settingName = AvailableSettings.CONNECTION_HANDLING,
						provider = CMTTest.ConnectionHandlingProvider.class
				)
		}

)
public class CMTTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@Test
	public void testConcurrent(SessionFactoryScope scope) throws Exception {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		sessionFactory.getStatistics().clear();
		assertEquals( 0, sessionFactory.getStatistics().getUpdateTimestampsCacheHitCount() );
		assertEquals( 0, sessionFactory.getStatistics().getUpdateTimestampsCachePutCount() );
		assertEquals( 0, sessionFactory.getStatistics().getUpdateTimestampsCacheMissCount() );
		assertNotNull( sessionFactory.getMappingMetamodel().getEntityDescriptor( "Item" ).getCacheAccessStrategy() );
		assertEquals( 0, sessionFactory.getStatistics().getEntityLoadCount() );

		final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			transactionManager.begin();
			Session s = sessionFactory.openSession();
			Map foo = new HashMap();
			foo.put( "name", "Foo" );
			foo.put( "description", "a big foo" );
			s.persist( "Item", foo );
			Map bar = new HashMap();
			bar.put( "name", "Bar" );
			bar.put( "description", "a small bar" );
			s.persist( "Item", bar );
			transactionManager.commit();
			assertEquals( 0, sessionFactory.getStatistics().getUpdateTimestampsCacheHitCount() );
			assertEquals(
					2,
					sessionFactory.getStatistics().getUpdateTimestampsCachePutCount()
			); // One preinvalidate & one invalidate
			assertEquals( 0, sessionFactory.getStatistics().getUpdateTimestampsCacheMissCount() );

			sessionFactory.getCache().evictEntityData( "Item" );

			transactionManager.begin();
			Session s1 = sessionFactory.openSession();
			foo = (Map) s1.get( "Item", "Foo" );
			//foo.put("description", "a big red foo");
			//s1.flush();
			Transaction tx = transactionManager.suspend();

			transactionManager.begin();
			Session s2 = sessionFactory.openSession();
			foo = (Map) s2.get( "Item", "Foo" );
			transactionManager.commit();

			transactionManager.resume( tx );
			transactionManager.commit();

			sessionFactory.getCache().evictEntityData( "Item" );

			transactionManager.begin();
			s1 = sessionFactory.openSession();
			s1.createQuery( "from Item" ).list();
			//foo.put("description", "a big red foo");
			//s1.flush();
			tx = transactionManager.suspend();

			transactionManager.begin();
			s2 = sessionFactory.openSession();
			s2.createQuery( "from Item" ).list();
			transactionManager.commit();

			transactionManager.resume( tx );
			transactionManager.commit();

			transactionManager.begin();
			s2 = sessionFactory.openSession();
			s2.createQuery( "from Item" ).list();
			transactionManager.commit();

			assertEquals( 7, sessionFactory.getStatistics().getEntityLoadCount() );
			assertEquals( 0, sessionFactory.getStatistics().getEntityFetchCount() );
			assertEquals( 3, sessionFactory.getStatistics().getQueryExecutionCount() );
			assertEquals( 0, sessionFactory.getStatistics().getQueryCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getQueryCacheMissCount() );
			assertEquals( 0, sessionFactory.getStatistics().getUpdateTimestampsCacheHitCount() );
			assertEquals( 2, sessionFactory.getStatistics().getUpdateTimestampsCachePutCount() );
		}
		finally {
			final Transaction transaction = transactionManager.getTransaction();
			if ( transaction != null && JtaStatusHelper.isActive( transaction.getStatus() ) ) {
				transactionManager.rollback();
			}
		}
	}

	@Test
	public void testConcurrentCachedQueries(SessionFactoryScope scope) throws Exception {
		final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
			sessionFactory.getStatistics().clear();
			transactionManager.begin();
			Session s = sessionFactory.openSession();
			Map foo = new HashMap();
			foo.put( "name", "Foo" );
			foo.put( "description", "a big foo" );
			s.persist( "Item", foo );
			Map bar = new HashMap();
			bar.put( "name", "Bar" );
			bar.put( "description", "a small bar" );
			s.persist( "Item", bar );
			transactionManager.commit();

			synchronized (this) {
				wait( 1000 );
			}

			sessionFactory.getStatistics().clear();

			sessionFactory.getCache().evictEntityData( "Item" );

			transactionManager.begin();
			Session s4 = sessionFactory.openSession();
			Transaction tx4 = transactionManager.suspend();

			transactionManager.begin();
			Session s1 = sessionFactory.openSession();
			List r1 = s1.createQuery( "from Item order by description" ).setCacheable( true ).list();
			assertEquals( r1.size(), 2 );
			Transaction tx1 = transactionManager.suspend();

			transactionManager.begin();
			Session s2 = sessionFactory.openSession();
			List r2 = s2.createQuery( "from Item order by description" ).setCacheable( true ).list();
			assertEquals( r2.size(), 2 );
			transactionManager.commit();

//		assertEquals( 2, sessionFactory().getStatistics().getSecondLevelCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheMissCount() );
			assertEquals( 2, sessionFactory.getStatistics().getEntityLoadCount() );
			assertEquals( 0, sessionFactory.getStatistics().getEntityFetchCount() );
			assertEquals( 1, sessionFactory.getStatistics().getQueryExecutionCount() );
			assertEquals( 1, sessionFactory.getStatistics().getQueryCachePutCount() );
			assertEquals( 1, sessionFactory.getStatistics().getQueryCacheHitCount() );
			assertEquals( 1, sessionFactory.getStatistics().getQueryCacheMissCount() );
			assertEquals( 1, sessionFactory.getStatistics().getUpdateTimestampsCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getUpdateTimestampsCachePutCount() );

			transactionManager.resume( tx1 );
			transactionManager.commit();

			transactionManager.begin();
			Session s3 = sessionFactory.openSession();
			s3.createQuery( "from Item order by description" ).setCacheable( true ).list();
			transactionManager.commit();

//		assertEquals( 4, sessionFactory().getStatistics().getSecondLevelCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheMissCount() );
			assertEquals( 2, sessionFactory.getStatistics().getEntityLoadCount() );
			assertEquals( 0, sessionFactory.getStatistics().getEntityFetchCount() );
			assertEquals( 1, sessionFactory.getStatistics().getQueryExecutionCount() );
			assertEquals( 1, sessionFactory.getStatistics().getQueryCachePutCount() );
			assertEquals( 2, sessionFactory.getStatistics().getQueryCacheHitCount() );
			assertEquals( 1, sessionFactory.getStatistics().getQueryCacheMissCount() );
			assertEquals( 2, sessionFactory.getStatistics().getUpdateTimestampsCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getUpdateTimestampsCachePutCount() );
			assertEquals( 0, sessionFactory.getStatistics().getUpdateTimestampsCacheMissCount() );

			transactionManager.resume( tx4 );
			List r4 = s4.createQuery( "from Item order by description" ).setCacheable( true ).list();
			assertEquals( r4.size(), 2 );
			transactionManager.commit();

//		assertEquals( 6, sessionFactory().getStatistics().getSecondLevelCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheMissCount() );
			assertEquals( 2, sessionFactory.getStatistics().getEntityLoadCount() );
			assertEquals( 0, sessionFactory.getStatistics().getEntityFetchCount() );
			assertEquals( 1, sessionFactory.getStatistics().getQueryExecutionCount() );
			assertEquals( 1, sessionFactory.getStatistics().getQueryCachePutCount() );
			assertEquals( 3, sessionFactory.getStatistics().getQueryCacheHitCount() );
			assertEquals( 1, sessionFactory.getStatistics().getQueryCacheMissCount() );
			assertEquals( 3, sessionFactory.getStatistics().getUpdateTimestampsCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getUpdateTimestampsCachePutCount() );
		}
		finally {
			final Transaction transaction = transactionManager.getTransaction();
			if ( transaction != null && JtaStatusHelper.isActive( transaction.getStatus() ) ) {
				transactionManager.rollback();
			}
		}
	}

	@Test
	@RequiresDialectFeature(
			feature = DialectFeatureChecks.DoesReadCommittedCauseWritersToBlockReadersCheck.class, reverse = true,
			comment = "write locks block readers"
	)
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "Cockroach uses SERIALIZABLE by default and seems to fail reading a row that is exclusively locked by a different TX")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix simply fails to obtain the lock with 'Could not do a physical-order read to fetch next row'")
	public void testConcurrentCachedDirtyQueries(SessionFactoryScope scope) throws Exception {
		final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
			transactionManager.begin();
			Session s = sessionFactory.openSession();
			Map foo = new HashMap();
			foo.put( "name", "Foo" );
			foo.put( "description", "a big foo" );
			s.persist( "Item", foo );
			Map bar = new HashMap();
			bar.put( "name", "Bar" );
			bar.put( "description", "a small bar" );
			s.persist( "Item", bar );
			transactionManager.commit();

			synchronized (this) {
				wait( 1000 );
			}

			sessionFactory.getStatistics().clear();
			sessionFactory.getCache().evictAllRegions();  // we need a clean 2L cache here.

			// open a TX and suspend it
			transactionManager.begin();
			Session s4 = sessionFactory.openSession();
			Transaction tx4 = transactionManager.suspend();

			// open a new TX and execute a query, this would fill the query cache.
			transactionManager.begin();
			Session s1 = sessionFactory.openSession();
			List r1 = s1.createQuery( "from Item order by description" ).setCacheable( true ).list();
			assertEquals( r1.size(), 2 );
			foo = (Map) r1.get( 0 );
			// update data and make query cache stale, but TX is suspended
			foo.put( "description", "a big red foo" );
			s1.flush();
			Transaction tx1 = transactionManager.suspend();

			// open a new TX and run query again
			// this TX is committed after query
			transactionManager.begin();
			Session s2 = sessionFactory.openSession();
			List r2 = s2.createQuery( "from Item order by description" ).setCacheable( true ).list();
			assertEquals( r2.size(), 2 );

			transactionManager.commit();

			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheMissCount() );
			assertEquals( 4, sessionFactory.getStatistics().getEntityLoadCount() );
			assertEquals( 0, sessionFactory.getStatistics().getEntityFetchCount() );
			assertEquals( 2, sessionFactory.getStatistics().getQueryExecutionCount() );
			assertEquals( 2, sessionFactory.getStatistics().getQueryCachePutCount() );
			assertEquals( 0, sessionFactory.getStatistics().getQueryCacheHitCount() );
			assertEquals( 2, sessionFactory.getStatistics().getQueryCacheMissCount() );

			// updateTimestampsCache put happens at two places
			// 1. {@link org.hibernate.engine.spi.ActionQueue#registerCleanupActions} calls preinvalidate
			// 2. {@link org.hibernate.engine.spi.ActionQueue.AfterTransactionCompletionProcessQueue#afterTransactionCompletion} calls invalidate
			// but since the TX which the update action happened is not committed yet, so there should be only 1 updateTimestamps put.
			assertEquals( 1, sessionFactory.getStatistics().getUpdateTimestampsCachePutCount() );

			// updateTimestampsCache hit only happens when the query cache data's timestamp is newer
			// than the timestamp of when update happens
			// since there is only 1 update action
			assertEquals( 1, sessionFactory.getStatistics().getUpdateTimestampsCacheHitCount() );

			transactionManager.resume( tx1 );
			transactionManager.commit();

			// update action's TX committed, so, invalidate is called, put new timestamp into UpdateTimestampsCache
			assertEquals( 2, sessionFactory.getStatistics().getUpdateTimestampsCachePutCount() );
			// but no more query cache lookup here, so it should still 1
			assertEquals( 1, sessionFactory.getStatistics().getUpdateTimestampsCacheHitCount() );

			transactionManager.begin();
			Session s3 = sessionFactory.openSession();
			s3.createQuery( "from Item order by description" ).setCacheable( true ).list();
			transactionManager.commit();

			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheMissCount() );
			assertEquals( 6, sessionFactory.getStatistics().getEntityLoadCount() );
			assertEquals( 0, sessionFactory.getStatistics().getEntityFetchCount() );
			assertEquals( 3, sessionFactory.getStatistics().getQueryExecutionCount() );
			assertEquals( 3, sessionFactory.getStatistics().getQueryCachePutCount() );
			assertEquals( 0, sessionFactory.getStatistics().getQueryCacheHitCount() );
			assertEquals( 3, sessionFactory.getStatistics().getQueryCacheMissCount() );
			// a new query cache hit and one more update timestamps cache hit, so should be 2
			assertEquals( 2, sessionFactory.getStatistics().getUpdateTimestampsCacheHitCount() );

			transactionManager.resume( tx4 );
			List r4 = s4.createQuery( "from Item order by description" ).setCacheable( true ).list();
			assertEquals( r4.size(), 2 );
			transactionManager.commit();

//		assertEquals( 2, sessionFactory().getStatistics().getSecondLevelCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheHitCount() );
			assertEquals( 0, sessionFactory.getStatistics().getSecondLevelCacheMissCount() );
			assertEquals( 6, sessionFactory.getStatistics().getEntityLoadCount() );
			assertEquals( 0, sessionFactory.getStatistics().getEntityFetchCount() );
			assertEquals( 3, sessionFactory.getStatistics().getQueryExecutionCount() );
			assertEquals( 3, sessionFactory.getStatistics().getQueryCachePutCount() );
			assertEquals( 1, sessionFactory.getStatistics().getQueryCacheHitCount() );
			assertEquals( 3, sessionFactory.getStatistics().getQueryCacheMissCount() );
			assertEquals( 3, sessionFactory.getStatistics().getUpdateTimestampsCacheHitCount() );
		}
		finally {
			final Transaction transaction = transactionManager.getTransaction();
			if ( transaction != null && JtaStatusHelper.isActive( transaction.getStatus() ) ) {
				transactionManager.rollback();
			}
		}
	}

	@Test
	public void testCMT(SessionFactoryScope scope) throws Exception {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		sessionFactory.getStatistics().clear();

		final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();

		try {
			transactionManager.begin();
			Session s = sessionFactory.openSession();
			transactionManager.commit();
			assertFalse( s.isOpen() );

			assertEquals( sessionFactory.getStatistics().getFlushCount(), 0 );
			assertEquals( sessionFactory.getStatistics().getEntityInsertCount(), 0 );

			transactionManager.begin();
			s = sessionFactory.openSession();
			transactionManager.rollback();
			assertFalse( s.isOpen() );

			transactionManager.begin();
			s = sessionFactory.openSession();
			Map item = new HashMap();
			item.put( "name", "The Item" );
			item.put( "description", "The only item we have" );
			s.persist( "Item", item );
			transactionManager.commit();
			assertFalse( s.isOpen() );
			assertEquals( sessionFactory.getStatistics().getFlushCount(), 1 );
			assertEquals( sessionFactory.getStatistics().getEntityInsertCount(), 1 );

			transactionManager.begin();
			s = sessionFactory.openSession();
			item = (Map) s.createQuery( "from Item" ).uniqueResult();
			assertNotNull( item );
			s.remove( item );
			transactionManager.commit();
			assertFalse( s.isOpen() );

			assertEquals( sessionFactory.getStatistics().getTransactionCount(), 4 );
			assertEquals( sessionFactory.getStatistics().getSuccessfulTransactionCount(), 3 );
			assertEquals( sessionFactory.getStatistics().getEntityDeleteCount(), 1 );
			assertEquals( sessionFactory.getStatistics().getEntityInsertCount(), 1 );
			assertEquals( sessionFactory.getStatistics().getSessionOpenCount(), 4 );
			assertEquals( sessionFactory.getStatistics().getSessionCloseCount(), 4 );
			assertEquals( sessionFactory.getStatistics().getQueryExecutionCount(), 1 );
			assertEquals( sessionFactory.getStatistics().getFlushCount(), 2 );
		}
		finally {
			final Transaction transaction = transactionManager.getTransaction();
			if ( transaction != null && JtaStatusHelper.isActive( transaction.getStatus() ) ) {
				transactionManager.rollback();
			}
		}
	}

	@Test
	public void testCurrentSession(SessionFactoryScope scope) throws Exception {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			transactionManager.begin();
			Session s = sessionFactory.getCurrentSession();
			Session s2 = sessionFactory.getCurrentSession();
			assertSame( s, s2 );
			transactionManager.commit();
			assertFalse( s.isOpen() );
		}
		finally {
			final Transaction transaction = transactionManager.getTransaction();
			if ( transaction != null && JtaStatusHelper.isActive( transaction.getStatus() ) ) {
				transactionManager.rollback();
			}
		}

		// TODO : would be nice to automate-test that the SF internal map actually gets cleaned up
		//      i verified that is does currently in my debugger...
	}

	@Test
	public void testCurrentSessionWithScroll(SessionFactoryScope scope) throws Exception {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			transactionManager.begin();
			Session s = sessionFactory.getCurrentSession();
			Map item1 = new HashMap();
			item1.put( "name", "Item - 1" );
			item1.put( "description", "The first item" );
			s.persist( "Item", item1 );

			Map item2 = new HashMap();
			item2.put( "name", "Item - 2" );
			item2.put( "description", "The second item" );
			s.persist( "Item", item2 );
			transactionManager.commit();

			// First, test partially scrolling the result with out closing
			transactionManager.begin();
			s = sessionFactory.getCurrentSession();
			ScrollableResults results = s.createQuery( "from Item" ).scroll();
			results.next();
			transactionManager.commit();

			// Next, test partially scrolling the result with closing
			transactionManager.begin();
			s = sessionFactory.getCurrentSession();
			results = s.createQuery( "from Item" ).scroll();
			results.next();
			results.close();
			transactionManager.commit();

			// Next, scroll the entire result (w/o closing)
			transactionManager.begin();
			s = sessionFactory.getCurrentSession();
			results = s.createQuery( "from Item" ).scroll();
			while ( results.next() ) {
				// do nothing
			}
			transactionManager.commit();

			// Next, scroll the entire result (closing)
			transactionManager.begin();
			s = sessionFactory.getCurrentSession();
			results = s.createQuery( "from Item" ).scroll();
			while ( results.next() ) {
				// do nothing
			}
			results.close();
			transactionManager.commit();
		}
		finally {
			final Transaction transaction = transactionManager.getTransaction();
			if ( transaction != null && JtaStatusHelper.isActive( transaction.getStatus() ) ) {
				transactionManager.rollback();
			}
		}
	}

	public static class JtaPlatfomProvider implements SettingProvider.Provider<Class> {
		@Override
		public Class getSetting() {
			return TestingJtaPlatformImpl.class;
		}
	}

	public static class ConnectionProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return JtaAwareConnectionProviderImpl.class.getName();
		}
	}

	public static class TransactionCoordinatorStrategyProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return JtaTransactionCoordinatorBuilderImpl.class.getName();
		}
	}

	public static class ConnectionHandlingProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT.toString();
		}
	}

}
