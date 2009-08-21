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
package org.hibernate.test.cache.infinispan.util;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;

/**
 * Utilities for cache testing.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public class CacheTestUtil {

   public static Configuration buildConfiguration(String regionPrefix, Class regionFactory, boolean use2ndLevel, boolean useQueries) {
      Configuration cfg = new Configuration();
      cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
      cfg.setProperty(Environment.USE_STRUCTURED_CACHE, "true");
      cfg.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, BatchModeTransactionManagerLookup.class.getName());

      cfg.setProperty(Environment.CACHE_REGION_FACTORY, regionFactory.getName());
      cfg.setProperty(Environment.CACHE_REGION_PREFIX, regionPrefix);
      cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, String.valueOf(use2ndLevel));
      cfg.setProperty(Environment.USE_QUERY_CACHE, String.valueOf(useQueries));

      return cfg;
   }

   public static Configuration buildLocalOnlyConfiguration(String regionPrefix, boolean use2ndLevel, boolean useQueries) {
      Configuration cfg = buildConfiguration(regionPrefix, InfinispanRegionFactory.class, use2ndLevel, useQueries);
      cfg.setProperty(InfinispanRegionFactory.INFINISPAN_CONFIG_RESOURCE_PROP,
               InfinispanRegionFactory.DEF_INFINISPAN_CONFIG_RESOURCE);
      return cfg;
   }
   
   public static Configuration buildCustomQueryCacheConfiguration(String regionPrefix, String queryCacheName) {
      Configuration cfg = buildConfiguration(regionPrefix, InfinispanRegionFactory.class, true, true);
      cfg.setProperty(InfinispanRegionFactory.QUERY_CACHE_RESOURCE_PROP, queryCacheName);
      return cfg;
   }

   public static InfinispanRegionFactory startRegionFactory(Configuration cfg) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {

      Settings settings = cfg.buildSettings();
      Properties properties = cfg.getProperties();

      String factoryType = cfg.getProperty(Environment.CACHE_REGION_FACTORY);
      Class factoryClass = Thread.currentThread().getContextClassLoader().loadClass(factoryType);
      InfinispanRegionFactory regionFactory = (InfinispanRegionFactory) factoryClass.newInstance();

      regionFactory.start(settings, properties);

      return regionFactory;
   }

   public static InfinispanRegionFactory startRegionFactory(Configuration cfg, CacheTestSupport testSupport)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
      InfinispanRegionFactory factory = startRegionFactory(cfg);
      testSupport.registerFactory(factory);
      return factory;
   }

   public static void stopRegionFactory(InfinispanRegionFactory factory, CacheTestSupport testSupport) {
      factory.stop();
      testSupport.unregisterFactory(factory);
   }

   /**
    * Supports easy creation of a TestSuite where a subclass' "FailureExpected" version of a base
    * test is included in the suite, while the base test is excluded. E.g. test class FooTestCase
    * includes method testBar(), while test class SubFooTestCase extends FooTestCase includes method
    * testBarFailureExcluded(). Passing SubFooTestCase.class to this method will return a suite that
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

   /**
    * Prevent instantiation.
    */
   private CacheTestUtil() {
   }

}
