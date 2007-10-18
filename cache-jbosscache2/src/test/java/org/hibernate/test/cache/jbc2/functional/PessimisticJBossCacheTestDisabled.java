package org.hibernate.test.cache.jbc2.functional;

import junit.framework.Test;

import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.jbc2.JBossCacheRegionFactory;
import org.hibernate.cache.jbc2.builder.SharedCacheInstanceManager;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * FIXME Move to hibernate-testsuite project and rename class x- "Disabled"
 * 
 * @author Brian Stansberry
 */
public class PessimisticJBossCacheTestDisabled extends AbstractQueryCacheFunctionalTestCase {

    // note that a lot of the fucntionality here is intended to be used
    // in creating specific tests for each CacheProvider that would extend
    // from a base test case (this) for common requirement testing...

    public PessimisticJBossCacheTestDisabled(String x) {
        super(x);
    }

    public static Test suite() {
        return new FunctionalTestClassTestSuite(PessimisticJBossCacheTestDisabled.class);
    }

    protected Class<? extends RegionFactory> getCacheRegionFactory() {
        return JBossCacheRegionFactory.class;
    }

    protected String getConfigResourceKey() {
        return SharedCacheInstanceManager.CACHE_RESOURCE_PROP;
    }

    protected String getConfigResourceLocation() {
        return "org/hibernate/test/cache/jbc2/functional/pessimistic-treecache.xml";
    }

}
