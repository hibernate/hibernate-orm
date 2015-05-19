/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import java.util.Map;
import javax.transaction.TransactionManager;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.Environment;

import org.hibernate.test.cache.infinispan.functional.classloader.Account;
import org.hibernate.test.cache.infinispan.functional.classloader.ClassLoaderTestDAO;
import org.junit.Test;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.test.TestingUtil;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * SessionRefreshTestCase.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class SessionRefreshTestCase extends DualNodeTestCase {
	private static final Logger log = Logger.getLogger( SessionRefreshTestCase.class );

	static int test = 0;
	private Cache localCache;

	@Override
	protected void configureSecondNode(StandardServiceRegistryBuilder ssrb) {
		super.configureSecondNode( ssrb );
		ssrb.applySetting( Environment.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Override
	protected void applyStandardSettings(Map settings) {
		super.applyStandardSettings( settings );
		settings.put( InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, getEntityCacheConfigName() );
	}

	protected String getEntityCacheConfigName() {
		return "entity";
	}

	@Override
	public String[] getMappings() {
		return new String[] {"cache/infinispan/functional/classloader/Account.hbm.xml"};
	}

	@Override
	protected void cleanupTransactionManagement() {
		// Don't clean up the managers, just the transactions
		// Managers are still needed by the long-lived caches
		DualNodeJtaTransactionManagerImpl.cleanupTransactions();
	}

	@Test
	public void testRefreshAfterExternalChange() throws Exception {
		// First session factory uses a cache
		CacheContainer localManager = ClusterAwareRegionFactory.getCacheManager( DualNodeTestCase.LOCAL );
		localCache = localManager.getCache( Account.class.getName() );
		TransactionManager localTM = DualNodeJtaTransactionManagerImpl.getInstance( DualNodeTestCase.LOCAL );
		SessionFactory localFactory = sessionFactory();

		// Second session factory doesn't; just needs a transaction manager
		TransactionManager remoteTM = DualNodeJtaTransactionManagerImpl.getInstance( DualNodeTestCase.REMOTE );
		SessionFactory remoteFactory = secondNodeEnvironment().getSessionFactory();

		ClassLoaderTestDAO dao0 = new ClassLoaderTestDAO( localFactory, localTM );
		ClassLoaderTestDAO dao1 = new ClassLoaderTestDAO( remoteFactory, remoteTM );

		Integer id = new Integer( 1 );
		dao0.createAccount( dao0.getSmith(), id, new Integer( 5 ), DualNodeTestCase.LOCAL );

		// Basic sanity check
		Account acct1 = dao1.getAccount( id );
		assertNotNull( acct1 );
		assertEquals( DualNodeTestCase.LOCAL, acct1.getBranch() );

		// This dao's session factory isn't caching, so cache won't see this change
		dao1.updateAccountBranch( id, DualNodeTestCase.REMOTE );

		// dao1's session doesn't touch the cache,
		// so reading from dao0 should show a stale value from the cache
		// (we check to confirm the cache is used)
		Account acct0 = dao0.getAccount( id );
		assertNotNull( acct0 );
		assertEquals( DualNodeTestCase.LOCAL, acct0.getBranch() );
		log.debug( "Contents when re-reading from local: " + TestingUtil.printCache( localCache ) );

		// Now call session.refresh and confirm we get the correct value
		acct0 = dao0.getAccountWithRefresh( id );
		assertNotNull( acct0 );
		assertEquals( DualNodeTestCase.REMOTE, acct0.getBranch() );
		log.debug( "Contents after refreshing in remote: " + TestingUtil.printCache( localCache ) );

		// Double check with a brand new session, in case the other session
		// for some reason bypassed the 2nd level cache
		ClassLoaderTestDAO dao0A = new ClassLoaderTestDAO( localFactory, localTM );
		Account acct0A = dao0A.getAccount( id );
		assertNotNull( acct0A );
		assertEquals( DualNodeTestCase.REMOTE, acct0A.getBranch() );
		log.debug( "Contents after creating a new session: " + TestingUtil.printCache( localCache ) );
	}
}
