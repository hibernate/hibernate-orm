/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.cache.jbc2.functional;

import org.hibernate.cache.RegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.junit.functional.FunctionalTestCase;

/**
 * Provides common configuration setups for cache testing.
 * 
 * @author Brian Stansberry
 */
public abstract class CacheTestCaseBase extends FunctionalTestCase {

    private static final String PREFER_IPV4STACK = "java.net.preferIPv4Stack";

    private String preferIPv4Stack;
    
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
        cfg.setProperty(Environment.CONNECTION_PROVIDER, org.hibernate.test.tm.ConnectionProviderImpl.class.getName());
        cfg.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, org.hibernate.test.tm.TransactionManagerLookupImpl.class.getName());

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

    @Override
    public void afterConfigurationBuilt(Mappings mappings, Dialect dialect) {
        
        super.afterConfigurationBuilt(mappings, dialect);
        
        // Try to ensure we use IPv4; otherwise cluster formation is very slow 
        preferIPv4Stack = System.getProperty(PREFER_IPV4STACK);
        System.setProperty(PREFER_IPV4STACK, "true");  
    }

    @Override
    protected void cleanupTest() throws Exception {
        try {
            super.cleanupTest();
        }
        finally {
            if (preferIPv4Stack == null)
                System.clearProperty(PREFER_IPV4STACK);
            else 
                System.setProperty(PREFER_IPV4STACK, preferIPv4Stack);
        }
        
    }
    
    
}
