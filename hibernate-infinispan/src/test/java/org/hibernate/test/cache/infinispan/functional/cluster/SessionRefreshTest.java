/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.Environment;

import org.hibernate.test.cache.infinispan.functional.entities.Account;
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
public class SessionRefreshTest extends DualNodeTest {
	private static final Logger log = Logger.getLogger( SessionRefreshTest.class );

	private Cache localCache;

	@Override
	public List<Object[]> getParameters() {
		return getParameters(true, true, false, true);
	}

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
		return new String[] {"cache/infinispan/functional/entities/Account.hbm.xml"};
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
		CacheContainer localManager = ClusterAwareRegionFactory.getCacheManager( DualNodeTest.LOCAL );
		localCache = localManager.getCache( Account.class.getName() );
		SessionFactory localFactory = sessionFactory();

		// Second session factory doesn't; just needs a transaction manager
		SessionFactory remoteFactory = secondNodeEnvironment().getSessionFactory();

		AccountDAO dao0 = new AccountDAO(useJta, localFactory );
		AccountDAO dao1 = new AccountDAO(useJta, remoteFactory );

		Integer id = new Integer( 1 );
		dao0.createAccount( dao0.getSmith(), id, new Integer( 5 ), DualNodeTest.LOCAL );

		// Basic sanity check
		Account acct1 = dao1.getAccount( id );
		assertNotNull( acct1 );
		assertEquals( DualNodeTest.LOCAL, acct1.getBranch() );

		// This dao's session factory isn't caching, so cache won't see this change
		dao1.updateAccountBranch( id, DualNodeTest.REMOTE );

		// dao1's session doesn't touch the cache,
		// so reading from dao0 should show a stale value from the cache
		// (we check to confirm the cache is used)
		Account acct0 = dao0.getAccount( id );
		assertNotNull( acct0 );
		assertEquals( DualNodeTest.LOCAL, acct0.getBranch() );
		log.debug( "Contents when re-reading from local: " + TestingUtil.printCache( localCache ) );

		// Now call session.refresh and confirm we get the correct value
		acct0 = dao0.getAccountWithRefresh( id );
		assertNotNull( acct0 );
		assertEquals( DualNodeTest.REMOTE, acct0.getBranch() );
		log.debug( "Contents after refreshing in remote: " + TestingUtil.printCache( localCache ) );

		// Double check with a brand new session, in case the other session
		// for some reason bypassed the 2nd level cache
		AccountDAO dao0A = new AccountDAO(useJta, localFactory );
		Account acct0A = dao0A.getAccount( id );
		assertNotNull( acct0A );
		assertEquals( DualNodeTest.REMOTE, acct0A.getBranch() );
		log.debug( "Contents after creating a new session: " + TestingUtil.printCache( localCache ) );
	}
}
