/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.test.TestingUtil;
import org.jboss.logging.Logger;
import org.junit.Test;

import org.hibernate.SessionFactory;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.cache.infinispan.functional.classloader.Account;
import org.hibernate.test.cache.infinispan.functional.classloader.ClassLoaderTestDAO;

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
	protected void configureSecondNode(Configuration cfg) {
		super.configureSecondNode( cfg );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Override
	protected void standardConfigure(Configuration cfg) {
		super.standardConfigure( cfg );
		cfg.setProperty( InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, getEntityCacheConfigName() );
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
