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
package org.hibernate.test.cache.jbc2;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.hibernate.cache.RegionFactory;
import org.hibernate.junit.UnitTestCase;
import org.hibernate.test.util.CacheTestSupport;
import org.jboss.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all non-functional tests of JBoss Cache integration.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractJBossCacheTestCase extends UnitTestCase {

    public static final String REGION_PREFIX = "test";
    
    private CacheTestSupport testSupport;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    public AbstractJBossCacheTestCase(String name) {
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

    
    /**
     * Supports easy creation of a TestSuite where a subclass' "FailureExpected"
     * version of a base test is included in the suite, while the base test
     * is excluded.  E.g. test class FooTestCase includes method testBar(), while test
     * class SubFooTestCase extends FooTestCase includes method testBarFailureExcluded().
     * Passing SubFooTestCase.class to this method will return a suite that
     * does not include testBar().
     * 
     * FIXME Move this to UnitTestCase
     */
    public static TestSuite createFailureExpectedSuite(Class testClass) {
       
       TestSuite allTests = new TestSuite(testClass);
       Set failureExpected = new HashSet();
       Enumeration tests = allTests.tests();
       while (tests.hasMoreElements()) {
          Test t = (Test) tests.nextElement();
          if (t instanceof TestCase) {
             String name = ((TestCase) t).getName();
             if (name.endsWith("FailureExpected"))
                failureExpected.add(name);
          }       
       }
       
       TestSuite result = new TestSuite();
       tests = allTests.tests();
       while (tests.hasMoreElements()) {
          Test t = (Test) tests.nextElement();
          if (t instanceof TestCase) {
             String name = ((TestCase) t).getName();
             if (!failureExpected.contains(name + "FailureExpected")) {
                result.addTest(t);
             }
          }       
       }
       
       return result;
    }
}