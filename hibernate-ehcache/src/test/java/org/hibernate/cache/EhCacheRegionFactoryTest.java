package org.hibernate.cache;

import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.testing.cache.BaseCacheRegionFactoryTestCase;

import junit.framework.Test;

/**
 * @author Alex Snaps
 */
public class EhCacheRegionFactoryTest extends BaseCacheRegionFactoryTestCase {

	public EhCacheRegionFactoryTest(String x) {
		super(x);
	}

	protected Class getCacheRegionFactory() {
		return EhCacheRegionFactory.class;
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite(EhCacheRegionFactoryTest.class);
	}

	public String getCacheConcurrencyStrategy() {
		return "read-write";
	}

	protected String getConfigResourceKey() {
		return Environment.CACHE_PROVIDER_CONFIG;
	}

	protected String getConfigResourceLocation() {
		return "ehcache.xml";
	}

	protected boolean useTransactionManager() {
		return false;
	}
}