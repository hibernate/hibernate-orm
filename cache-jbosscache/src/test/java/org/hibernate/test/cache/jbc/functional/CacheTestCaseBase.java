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
package org.hibernate.test.cache.jbc.functional;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides common configuration setups for cache testing.
 * 
 * @author Brian Stansberry
 */
public abstract class CacheTestCaseBase extends FunctionalTestCase {

    private static final Logger log = LoggerFactory.getLogger( CacheTestCaseBase.class );
    
    private static final String PREFER_IPV4STACK = "java.net.preferIPv4Stack";

    private String preferIPv4Stack;
    
    // note that a lot of the functionality here is intended to be used
    // in creating specific tests for each CacheProvider that would extend
    // from a base test case (this) for common requirement testing...

    public CacheTestCaseBase(String x) {
        super(x);
    }

    public String[] getMappings() {
        return new String[] {
				"cache/jbc/functional/Item.hbm.xml",
				"cache/jbc/functional/Customer.hbm.xml",
				"cache/jbc/functional/Contact.hbm.xml"
		};
    }

    public void configure(Configuration cfg) {
        super.configure(cfg);

        if (getRegionPrefix() != null) {
            cfg.setProperty(Environment.CACHE_REGION_PREFIX, getRegionPrefix());
        }
        
        cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
        cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
        cfg.setProperty(Environment.USE_STRUCTURED_CACHE, "true");
        cfg.setProperty(Environment.CACHE_REGION_FACTORY, getCacheRegionFactory().getName());

        cfg.setProperty(Environment.USE_QUERY_CACHE, String.valueOf(getUseQueryCache()));
        cfg.setProperty(Environment.CONNECTION_PROVIDER, getConnectionProviderClass().getName());
        cfg.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, getTransactionManagerLookupClass().getName());
        
        Class<?> transactionFactory = getTransactionFactoryClass();
        if (transactionFactory != null)
            cfg.setProperty( Environment.TRANSACTION_STRATEGY, transactionFactory.getName() );
        
        configureCacheFactory(cfg);
    }  
    
    protected String getRegionPrefix() {
        return "test";
    }
    
    protected String getPrefixedRegionName(String regionName)
    {
       String prefix = getRegionPrefix() == null ? "" : getRegionPrefix() + ".";
       return prefix + regionName;
    }

    public String getCacheConcurrencyStrategy() {
        return "transactional";
    }    

    /**
     * Apply any region-factory specific configurations.
     * 
     * @param cfg the Configuration to update.
     */
    protected abstract void configureCacheFactory(Configuration cfg);

    protected abstract Class<?> getCacheRegionFactory();

    protected abstract boolean getUseQueryCache();
    
    protected Class<?> getConnectionProviderClass() {
        return org.hibernate.test.tm.ConnectionProviderImpl.class;
    }
    
    protected Class<?> getTransactionManagerLookupClass() {
        return org.hibernate.test.tm.TransactionManagerLookupImpl.class;
    }
    
    protected Class<?> getTransactionFactoryClass() {
        return null;
    }

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
    
    protected void sleep(long ms) {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
            log.warn("Interrupted during sleep", e);
        }
    }
    
    
}
