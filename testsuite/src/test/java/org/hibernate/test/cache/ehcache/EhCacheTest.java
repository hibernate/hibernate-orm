//$Id: $
package org.hibernate.test.cache.ehcache;

import junit.framework.Test;

import org.hibernate.cache.EhCacheProvider;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.cache.BaseCacheProviderTestCase;

/**
 * @author Emmanuel Bernard
 */
public class EhCacheTest extends BaseCacheProviderTestCase {

	// note that a lot of the fucntionality here is intended to be used
	// in creating specific tests for each CacheProvider that would extend
	// from a base test case (this) for common requirement testing...

	public EhCacheTest(String x) {
		super( x );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( EhCacheTest.class );
	}

	public String getCacheConcurrencyStrategy() {
		return "read-write";
	}

	protected Class getCacheProvider() {
		return EhCacheProvider.class;
	}

	protected String getConfigResourceKey() {
		return Environment.CACHE_PROVIDER_CONFIG;
	}

	protected String getConfigResourceLocation() {
		return "org/hibernate/test/cache/ehcache/ehcache.xml";
	}

	protected boolean useTransactionManager() {
		return false;
	}

}
