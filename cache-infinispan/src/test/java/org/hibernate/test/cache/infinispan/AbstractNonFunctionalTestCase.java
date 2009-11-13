/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
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
package org.hibernate.test.cache.infinispan;

import java.util.Set;

import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.infinispan.util.CacheHelper;
import org.hibernate.junit.UnitTestCase;
import org.hibernate.test.cache.infinispan.util.CacheTestSupport;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all non-functional tests of Infinispan integration.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractNonFunctionalTestCase extends UnitTestCase {

    public static final String REGION_PREFIX = "test";
    
    private CacheTestSupport testSupport;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    public AbstractNonFunctionalTestCase(String name) {
        super(name);
        testSupport = new CacheTestSupport(log);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        testSupport.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        
        testSupport.tearDown();
    }

    protected void registerCache(Cache cache) {
        testSupport.registerCache(cache);
    }

    protected void unregisterCache(Cache cache) {
        testSupport.unregisterCache(cache);
    }

    protected void registerFactory(RegionFactory factory) {
        testSupport.registerFactory(factory);
    }

    protected void unregisterFactory(RegionFactory factory) {
        testSupport.unregisterFactory(factory);
    }

    protected CacheTestSupport getCacheTestSupport() {
        return testSupport;
    }

    protected void sleep(long ms) {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
            log.warn("Interrupted during sleep", e);
        }
    }
    
    protected void avoidConcurrentFlush() {
        testSupport.avoidConcurrentFlush();
    }

    protected int getValidKeyCount(Set keys) {
       int result = 0;
       for (Object key : keys) {
          if (!(CacheHelper.isEvictAllNotification(key))) {
             result++;
          }
       }
       return result;
   }

}