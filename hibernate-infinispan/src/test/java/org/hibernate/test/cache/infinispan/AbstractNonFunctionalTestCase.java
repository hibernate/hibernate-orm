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

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.test.cache.infinispan.util.CacheTestSupport;

/**
 * Base class for all non-functional tests of Infinispan integration.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractNonFunctionalTestCase extends org.hibernate.testing.junit4.BaseUnitTestCase {
   private static final Logger log = Logger.getLogger(AbstractNonFunctionalTestCase.class);

   public static final String REGION_PREFIX = "test";

   private static final String PREFER_IPV4STACK = "java.net.preferIPv4Stack";
   private String preferIPv4Stack;
   private static final String JGROUPS_CFG_FILE = "hibernate.cache.infinispan.jgroups_cfg";
   private String jgroupsCfgFile;

   private CacheTestSupport testSupport = new CacheTestSupport();

   @Before
   public void prepareCacheSupport() throws Exception {
      preferIPv4Stack = System.getProperty(PREFER_IPV4STACK);
      System.setProperty(PREFER_IPV4STACK, "true");
      jgroupsCfgFile = System.getProperty(JGROUPS_CFG_FILE);
      System.setProperty(JGROUPS_CFG_FILE, "2lc-test-tcp.xml");

      testSupport.setUp();
   }

   @After
   public void releaseCachSupport() throws Exception {
      testSupport.tearDown();

      if (preferIPv4Stack == null) {
         System.clearProperty(PREFER_IPV4STACK);
      } else {
         System.setProperty(PREFER_IPV4STACK, preferIPv4Stack);
      }
      
      if (jgroupsCfgFile == null)
         System.clearProperty(JGROUPS_CFG_FILE);
      else
         System.setProperty(JGROUPS_CFG_FILE, jgroupsCfgFile);
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
      } catch (InterruptedException e) {
         log.warn("Interrupted during sleep", e);
      }
   }

   protected void avoidConcurrentFlush() {
      testSupport.avoidConcurrentFlush();
   }

   protected int getValidKeyCount(Set keys) {
      return keys.size();
   }

}