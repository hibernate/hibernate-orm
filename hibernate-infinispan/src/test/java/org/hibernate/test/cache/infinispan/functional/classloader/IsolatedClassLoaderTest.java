/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.classloader;

import java.util.Map;
import javax.transaction.TransactionManager;

import org.hibernate.SessionFactory;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.internal.StandardQueryCache;

import org.hibernate.test.cache.infinispan.functional.cluster.ClusterAwareRegionFactory;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeJtaTransactionManagerImpl;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests entity and query caching when class of objects being cached are not visible to Infinispan's classloader. Also serves as a
 * general integration test.
 * <p/>
 * This test stores an object (AccountHolder) that isn't visible to the Infinispan classloader in the cache in two places: 1) As
 * part of the value tuple in an Account entity 2) As part of the FQN in a query cache entry (see query in
 * ClassLoaderTestDAO.getBranch())
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class IsolatedClassLoaderTest extends DualNodeTestCase {
	private static final Logger log = Logger.getLogger( IsolatedClassLoaderTest.class );

	protected static final long SLEEP_TIME = 300L;

	private Cache localQueryCache;
	private CacheAccessListener localQueryListener;

	private Cache remoteQueryCache;
	private CacheAccessListener remoteQueryListener;

	private static ClassLoader originalTCCL;

//   private static ClassLoader visibleClassesCl;

	@BeforeClass
	public static void prepareClassLoader() {
		final String packageName = IsolatedClassLoaderTest.class.getPackage().getName();
		final String[] classes = new String[] {packageName + ".Account", packageName + ".AccountHolder"};
		originalTCCL = Thread.currentThread().getContextClassLoader();
		// Most likely, it will point to system classloader
		ClassLoader parent = originalTCCL == null ? IsolatedClassLoaderTest.class.getClassLoader() : originalTCCL;

		// First, create a classloader where classes won't be found
		ClassLoader selectedTCCL = new SelectedClassnameClassLoader( null, null, classes, parent );

		// Now, make the class visible to the test driver
		SelectedClassnameClassLoader visible = new SelectedClassnameClassLoader( classes, null, null, selectedTCCL );
		Thread.currentThread().setContextClassLoader( visible );
//      visibleClassesCl = new SelectedClassnameClassLoader(classes, null, null, selectedTCCL);
//      Thread.currentThread().setContextClassLoader(selectedTCCL);
	}

	@AfterClass
	public static void resetClassLoader() {
		ClusterAwareRegionFactory.clearCacheManagers();
		DualNodeJtaTransactionManagerImpl.cleanupTransactions();
		DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
		Thread.currentThread().setContextClassLoader( originalTCCL );
	}

	@Override
	public String[] getMappings() {
		return new String[] {"cache/infinispan/functional/classloader/Account.hbm.xml"};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void applyStandardSettings(Map settings) {
		super.applyStandardSettings( settings );

		settings.put( InfinispanRegionFactory.QUERY_CACHE_RESOURCE_PROP, "replicated-query" );
		settings.put( "hibernate.cache.infinispan.AccountRegion.cfg", "replicated-query" );
	}

	@Override
	protected void cleanupTransactionManagement() {
		// Don't clean up the managers, just the transactions
		// Managers are still needed by the long-lived caches
		DualNodeJtaTransactionManagerImpl.cleanupTransactions();
	}

	@Override
	protected void cleanupTest() throws Exception {
		try {
			// Clear the local account cache
			sessionFactory().getCache().evictEntityRegion( Account.class.getName() );
			if ( localQueryCache != null && localQueryListener != null ) {
				localQueryCache.removeListener( localQueryListener );
			}
			if ( remoteQueryCache != null && remoteQueryListener != null ) {
				remoteQueryCache.removeListener( remoteQueryListener );
			}
		}
		finally {
			super.cleanupTest();
		}
	}

	@Ignore("Infinispan caches now use whichever classloader is associated on " +
			"construction, i.e. deployment JPA app, so does not rely on TCCL.")
	@Test
	public void testIsolatedSetup() throws Exception {
		// Bind a listener to the "local" cache
		// Our region factory makes its CacheManager available to us
		CacheContainer localManager = ClusterAwareRegionFactory.getCacheManager( DualNodeTestCase.LOCAL );
		Cache localReplicatedCache = localManager.getCache( "replicated-entity" );

		// Bind a listener to the "remote" cache
		CacheContainer remoteManager = ClusterAwareRegionFactory.getCacheManager( DualNodeTestCase.REMOTE );
		Cache remoteReplicatedCache = remoteManager.getCache( "replicated-entity" );

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader( cl.getParent() );
		log.info( "TCCL is " + cl.getParent() );

		Account acct = new Account();
		acct.setAccountHolder( new AccountHolder() );

		try {
			localReplicatedCache.put( "isolated1", acct );
			// With lazy deserialization, retrieval in remote forces class resolution
			remoteReplicatedCache.get( "isolated1" );
			fail( "Should not have succeeded in putting acct -- classloader not isolated" );
		}
		catch (Exception e) {
			if ( e.getCause() instanceof ClassNotFoundException ) {
				log.info( "Caught exception as desired", e );
			}
			else {
				throw new IllegalStateException( "Unexpected exception", e );
			}
		}

		Thread.currentThread().setContextClassLoader( cl );
		log.info( "TCCL is " + cl );
		localReplicatedCache.put( "isolated2", acct );
		assertEquals( acct.getClass().getName(), remoteReplicatedCache.get( "isolated2" ).getClass().getName() );
	}

	@Ignore("Infinispan caches now use whichever classloader is associated on " +
			"construction, i.e. deployment JPA app, so does not rely on TCCL.")
	@Test
	public void testClassLoaderHandlingNamedQueryRegion() throws Exception {
		rebuildSessionFactory();
		queryTest( true );
	}

	@Ignore("Infinispan caches now use whichever classloader is associated on " +
			"construction, i.e. deployment JPA app, so does not rely on TCCL.")
	@Test
	public void testClassLoaderHandlingStandardQueryCache() throws Exception {
		rebuildSessionFactory();
		queryTest( false );
	}

	protected void queryTest(boolean useNamedRegion) throws Exception {
		// Bind a listener to the "local" cache
		// Our region factory makes its CacheManager available to us
		EmbeddedCacheManager localManager = ClusterAwareRegionFactory.getCacheManager( DualNodeTestCase.LOCAL );
		// Bind a listener to the "remote" cache
		EmbeddedCacheManager remoteManager = ClusterAwareRegionFactory.getCacheManager( DualNodeTestCase.REMOTE );
		String cacheName;
		if ( useNamedRegion ) {
			cacheName = "AccountRegion"; // As defined by ClassLoaderTestDAO via calls to query.setCacheRegion
			// Define cache configurations for region early to avoid ending up with local caches for this region
			localManager.defineConfiguration(
					cacheName,
					localManager.getCacheConfiguration( "replicated-query" )
			);
			remoteManager.defineConfiguration(
					cacheName,
					remoteManager.getCacheConfiguration( "replicated-query" )
			);
		}
		else {
			cacheName = "replicated-query";
		}

		localQueryCache = localManager.getCache( cacheName );
		localQueryListener = new CacheAccessListener();
		localQueryCache.addListener( localQueryListener );

		TransactionManager localTM = DualNodeJtaTransactionManagerImpl.getInstance( DualNodeTestCase.LOCAL );

		remoteQueryCache = remoteManager.getCache( cacheName );
		remoteQueryListener = new CacheAccessListener();
		remoteQueryCache.addListener( remoteQueryListener );

		TransactionManager remoteTM = DualNodeJtaTransactionManagerImpl.getInstance( DualNodeTestCase.REMOTE );

		SessionFactory localFactory = sessionFactory();
		SessionFactory remoteFactory = secondNodeEnvironment().getSessionFactory();

		ClassLoaderTestDAO dao0 = new ClassLoaderTestDAO( localFactory, localTM );
		ClassLoaderTestDAO dao1 = new ClassLoaderTestDAO( remoteFactory, remoteTM );

		// Initial ops on node 0
		setupEntities( dao0 );

		String branch = "63088";
		// Query on post code count
		assertEquals( branch + " has correct # of accounts", 6, dao0.getCountForBranch( branch, useNamedRegion ) );

		assertEquals( "Query cache used", 1, localQueryListener.getSawRegionModificationCount() );
		localQueryListener.clearSawRegionModification();

//      log.info("First query (get count for branch + " + branch + " ) on node0 done, contents of local query cache are: " + TestingUtil.printCache(localQueryCache));

		// Sleep a bit to allow async repl to happen
		sleep( SLEEP_TIME );

		assertEquals( "Query cache used", 1, remoteQueryListener.getSawRegionModificationCount() );
		remoteQueryListener.clearSawRegionModification();

		// Do query again from node 1
		log.info( "Repeat first query (get count for branch + " + branch + " ) on remote node" );
		assertEquals( "63088 has correct # of accounts", 6, dao1.getCountForBranch( branch, useNamedRegion ) );
		assertEquals( "Query cache used", 1, remoteQueryListener.getSawRegionModificationCount() );
		remoteQueryListener.clearSawRegionModification();

		sleep( SLEEP_TIME );

		assertEquals( "Query cache used", 1, localQueryListener.getSawRegionModificationCount() );
		localQueryListener.clearSawRegionModification();

		log.info( "First query on node 1 done" );

		// Sleep a bit to allow async repl to happen
		sleep( SLEEP_TIME );

		// Do some more queries on node 0
		log.info( "Do query Smith's branch" );
		assertEquals( "Correct branch for Smith", "94536", dao0.getBranch( dao0.getSmith(), useNamedRegion ) );
		log.info( "Do query Jone's balance" );
		assertEquals( "Correct high balances for Jones", 40, dao0.getTotalBalance( dao0.getJones(), useNamedRegion ) );

		assertEquals( "Query cache used", 2, localQueryListener.getSawRegionModificationCount() );
		localQueryListener.clearSawRegionModification();
//      // Clear the access state
//      localQueryListener.getSawRegionAccess("???");

		log.info( "Second set of queries on node0 done" );

		// Sleep a bit to allow async repl to happen
		sleep( SLEEP_TIME );

		// Check if the previous queries replicated
		assertEquals( "Query cache remotely modified", 2, remoteQueryListener.getSawRegionModificationCount() );
		remoteQueryListener.clearSawRegionModification();

		log.info( "Repeat second set of queries on node1" );

		// Do queries again from node 1
		log.info( "Again query Smith's branch" );
		assertEquals( "Correct branch for Smith", "94536", dao1.getBranch( dao1.getSmith(), useNamedRegion ) );
		log.info( "Again query Jone's balance" );
		assertEquals( "Correct high balances for Jones", 40, dao1.getTotalBalance( dao1.getJones(), useNamedRegion ) );

		// Should be no change; query was already there
		assertEquals( "Query cache modified", 0, remoteQueryListener.getSawRegionModificationCount() );
		assertEquals( "Query cache accessed", 2, remoteQueryListener.getSawRegionAccessCount() );
		remoteQueryListener.clearSawRegionAccess();

		log.info( "Second set of queries on node1 done" );

		// allow async to propagate
		sleep( SLEEP_TIME );

		// Modify underlying data on node 1
		modifyEntities( dao1 );

		// allow async timestamp change to propagate
		sleep( SLEEP_TIME );

		// Confirm query results are correct on node 0
		assertEquals( "63088 has correct # of accounts", 7, dao0.getCountForBranch( "63088", useNamedRegion ) );
		assertEquals( "Correct branch for Smith", "63088", dao0.getBranch( dao0.getSmith(), useNamedRegion ) );
		assertEquals( "Correct high balances for Jones", 50, dao0.getTotalBalance( dao0.getJones(), useNamedRegion ) );
		log.info( "Third set of queries on node0 done" );
	}

	protected void setupEntities(ClassLoaderTestDAO dao) throws Exception {
		dao.cleanup();

		dao.createAccount( dao.getSmith(), new Integer( 1001 ), new Integer( 5 ), "94536" );
		dao.createAccount( dao.getSmith(), new Integer( 1002 ), new Integer( 15 ), "94536" );
		dao.createAccount( dao.getSmith(), new Integer( 1003 ), new Integer( 20 ), "94536" );

		dao.createAccount( dao.getJones(), new Integer( 2001 ), new Integer( 5 ), "63088" );
		dao.createAccount( dao.getJones(), new Integer( 2002 ), new Integer( 15 ), "63088" );
		dao.createAccount( dao.getJones(), new Integer( 2003 ), new Integer( 20 ), "63088" );

		dao.createAccount( dao.getBarney(), new Integer( 3001 ), new Integer( 5 ), "63088" );
		dao.createAccount( dao.getBarney(), new Integer( 3002 ), new Integer( 15 ), "63088" );
		dao.createAccount( dao.getBarney(), new Integer( 3003 ), new Integer( 20 ), "63088" );

		log.info( "Standard entities created" );
	}

	protected void resetRegionUsageState(CacheAccessListener localListener, CacheAccessListener remoteListener) {
		String stdName = StandardQueryCache.class.getName();
		String acctName = Account.class.getName();

		localListener.getSawRegionModification( stdName );
		localListener.getSawRegionModification( acctName );

		localListener.getSawRegionAccess( stdName );
		localListener.getSawRegionAccess( acctName );

		remoteListener.getSawRegionModification( stdName );
		remoteListener.getSawRegionModification( acctName );

		remoteListener.getSawRegionAccess( stdName );
		remoteListener.getSawRegionAccess( acctName );

		log.info( "Region usage state cleared" );
	}

	protected void modifyEntities(ClassLoaderTestDAO dao) throws Exception {
		dao.updateAccountBranch( 1001, "63088" );
		dao.updateAccountBalance( 2001, 15 );

		log.info( "Entities modified" );
	}
}
