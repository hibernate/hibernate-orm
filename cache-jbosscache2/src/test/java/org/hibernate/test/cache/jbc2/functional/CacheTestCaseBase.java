package org.hibernate.test.cache.jbc2.functional;

import org.hibernate.cache.RegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.test.tm.DummyConnectionProvider;
import org.hibernate.test.tm.DummyTransactionManagerLookup;

/**
 * Provides common configuration setups for cache testing.
 * 
 * @author Brian Stansberry
 */
public abstract class CacheTestCaseBase extends FunctionalTestCase {

    // note that a lot of the functionality here is intended to be used
    // in creating specific tests for each CacheProvider that would extend
    // from a base test case (this) for common requirement testing...

    public CacheTestCaseBase(String x) {
        super(x);
    }

    public String[] getMappings() {
        return new String[] { "cache/jbc2/functional/Item.hbm.xml" };
    }

    public void configure(Configuration cfg) {
        super.configure(cfg);

        cfg.setProperty(Environment.CACHE_REGION_PREFIX, "test");
        cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
        cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
        cfg.setProperty(Environment.USE_STRUCTURED_CACHE, "true");
        cfg.setProperty(Environment.CACHE_REGION_FACTORY, getCacheRegionFactory().getName());

        cfg.setProperty(Environment.USE_QUERY_CACHE, String.valueOf(getUseQueryCache()));
        cfg.setProperty(Environment.CONNECTION_PROVIDER, DummyConnectionProvider.class.getName());
        cfg.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, DummyTransactionManagerLookup.class.getName());

        configureCacheFactory(cfg);
    }

    public String getCacheConcurrencyStrategy() {
        return "transactional";
    }

    /**
     * The cache provider to be tested.
     * 
     * @return The cache provider.
     */
    protected void configureCacheFactory(Configuration cfg) {
        if (getConfigResourceKey() != null) {
            cfg.setProperty(getConfigResourceKey(), getConfigResourceLocation());
        }
    }

    protected abstract Class<? extends RegionFactory> getCacheRegionFactory();

    protected abstract boolean getUseQueryCache();

    /**
     * For provider-specific configuration, the name of the property key the
     * provider expects.
     * 
     * @return The provider-specific config key.
     */
    protected String getConfigResourceKey() {
        return Environment.CACHE_REGION_FACTORY;
    }

    /**
     * For provider-specific configuration, the resource location of that config
     * resource.
     * 
     * @return The config resource location.
     */
    protected abstract String getConfigResourceLocation();
}
