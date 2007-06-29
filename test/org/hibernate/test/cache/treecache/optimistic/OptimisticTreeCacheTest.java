package org.hibernate.test.cache.treecache.optimistic;

import junit.framework.Test;
import org.jboss.cache.Fqn;
import org.jboss.cache.TreeCache;
import org.jboss.cache.config.Option;
import org.jboss.cache.optimistic.DataVersion;

import org.hibernate.cache.OptimisticTreeCacheProvider;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.cache.BaseCacheProviderTestCase;
import org.hibernate.test.tm.DummyTransactionManager;

/**
 * @author Steve Ebersole
 */
public class OptimisticTreeCacheTest extends BaseCacheProviderTestCase {

	// note that a lot of the fucntionality here is intended to be used
	// in creating specific tests for each CacheProvider that would extend
	// from a base test case (this) for common requirement testing...

	public OptimisticTreeCacheTest(String x) {
		super( x );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( OptimisticTreeCacheTest.class );
	}

	public String getCacheConcurrencyStrategy() {
		return "transactional";
	}

	protected Class getCacheProvider() {
		return OptimisticTreeCacheProvider.class;
	}

	protected String getConfigResourceKey() {
		return Environment.CACHE_PROVIDER_CONFIG;
	}

	protected String getConfigResourceLocation() {
		return "org/hibernate/test/cache/treecache/optimistic/treecache.xml";
	}

	protected boolean useTransactionManager() {
		return true;
	}

	public void testCacheLevelStaleWritesFail() throws Throwable {
		Fqn fqn = new Fqn( "whatever" );
		TreeCache treeCache = ( ( OptimisticTreeCacheProvider ) sfi().getSettings().getCacheProvider() ).getUnderlyingCache();

		Long long1 = new Long(1);
		Long long2 = new Long(2);

		try {
			System.out.println( "****************************************************************" );
			DummyTransactionManager.INSTANCE.begin();
			treeCache.put( fqn, "ITEM", long1, ManualDataVersion.gen( 1 ) );
			DummyTransactionManager.INSTANCE.commit();

			System.out.println( "****************************************************************" );
			DummyTransactionManager.INSTANCE.begin();
			treeCache.put( fqn, "ITEM", long2, ManualDataVersion.gen( 2 ) );
			DummyTransactionManager.INSTANCE.commit();

			try {
				System.out.println( "****************************************************************" );
				DummyTransactionManager.INSTANCE.begin();
				treeCache.put( fqn, "ITEM", long1, ManualDataVersion.gen( 1 ) );
				DummyTransactionManager.INSTANCE.commit();
				fail( "stale write allowed" );
			}
			catch( Throwable ignore ) {
				// expected behavior
				DummyTransactionManager.INSTANCE.rollback();
			}

			Long current = ( Long ) treeCache.get( fqn, "ITEM" );
			assertEquals( "unexpected current value", 2, current.longValue() );
		}
		finally {
			try {
				treeCache.remove( fqn, "ITEM" );
			}
			catch( Throwable ignore ) {
			}
		}
	}

	private static class ManualDataVersion implements DataVersion {
		private final int version;

		public ManualDataVersion(int version) {
			this.version = version;
		}

		public boolean newerThan(DataVersion dataVersion) {
			return this.version > ( ( ManualDataVersion ) dataVersion ).version;
		}

		public static Option gen(int version) {
			ManualDataVersion mdv = new ManualDataVersion( version );
			Option option = new Option();
			option.setDataVersion( mdv );
			return option;
		}
	}
}
