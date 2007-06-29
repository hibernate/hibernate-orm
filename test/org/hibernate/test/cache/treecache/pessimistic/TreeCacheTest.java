package org.hibernate.test.cache.treecache.pessimistic;

import junit.framework.Test;

import org.hibernate.cache.TreeCacheProvider;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.cache.BaseCacheProviderTestCase;

/**
 * @author Steve Ebersole
 */
public class TreeCacheTest extends BaseCacheProviderTestCase {

	// note that a lot of the fucntionality here is intended to be used
	// in creating specific tests for each CacheProvider that would extend
	// from a base test case (this) for common requirement testing...

	public TreeCacheTest(String x) {
		super( x );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( TreeCacheTest.class );
	}

	public String getCacheConcurrencyStrategy() {
		return "transactional";
	}

	protected Class getCacheProvider() {
		return TreeCacheProvider.class;
	}

	protected String getConfigResourceKey() {
		return Environment.CACHE_PROVIDER_CONFIG;
	}

	protected String getConfigResourceLocation() {
		return "org/hibernate/test/cache/treecache/pessimistic/treecache.xml";
	}

	protected boolean useTransactionManager() {
		return true;
	}

}
