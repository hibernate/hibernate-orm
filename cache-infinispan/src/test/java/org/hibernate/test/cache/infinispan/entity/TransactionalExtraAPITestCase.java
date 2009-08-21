/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or it's affiliates, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.test.cache.infinispan.entity;

import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.infinispan.AbstractNonFunctionalTestCase;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;

/**
 * Tests for the "extra API" in EntityRegionAccessStrategy;.
 * <p>
 * By "extra API" we mean those methods that are superfluous to the 
 * function of the JBC integration, where the impl is a no-op or a static
 * false return value, UnsupportedOperationException, etc.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class TransactionalExtraAPITestCase extends AbstractNonFunctionalTestCase {

   public TransactionalExtraAPITestCase(String name) {
      super(name);
   }
   
   public static final String REGION_NAME = "test/com.foo.test";
   public static final String KEY = "KEY";
   public static final String VALUE1 = "VALUE1";
   public static final String VALUE2 = "VALUE2";
   
   private static EntityRegionAccessStrategy localAccessStrategy;
   
   private static boolean optimistic;
   
   protected void setUp() throws Exception {
       super.setUp();
       
       if (getEntityAccessStrategy() == null) {
           Configuration cfg = createConfiguration();
           InfinispanRegionFactory rf  = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
           
           // Sleep a bit to avoid concurrent FLUSH problem
           avoidConcurrentFlush();
           
           EntityRegion localEntityRegion = rf.buildEntityRegion(REGION_NAME, cfg.getProperties(), null);
           setEntityRegionAccessStrategy(localEntityRegion.buildAccessStrategy(getAccessType()));
       }
   }

   protected void tearDown() throws Exception {
       
       super.tearDown();
   }
   
   protected Configuration createConfiguration() {
       Configuration cfg = CacheTestUtil.buildConfiguration(REGION_PREFIX, InfinispanRegionFactory.class, true, false);
       cfg.setProperty(InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, getCacheConfigName());
       return cfg;
   }
   
   protected String getCacheConfigName() {
       return "entity";
   }
   
   protected AccessType getAccessType() {
       return AccessType.TRANSACTIONAL;
   }
   
   protected EntityRegionAccessStrategy getEntityAccessStrategy() {
       return localAccessStrategy;
   }
   
   protected void setEntityRegionAccessStrategy(EntityRegionAccessStrategy strategy) {
       localAccessStrategy = strategy;
   }

   /**
    * Test method for {@link TransactionalAccess#lockItem(java.lang.Object, java.lang.Object)}.
    */
   public void testLockItem() {
       assertNull(getEntityAccessStrategy().lockItem(KEY, new Integer(1)));
   }

   /**
    * Test method for {@link TransactionalAccess#lockRegion()}.
    */
   public void testLockRegion() {
       assertNull(getEntityAccessStrategy().lockRegion());
   }

   /**
    * Test method for {@link TransactionalAccess#unlockItem(java.lang.Object, org.hibernate.cache.access.SoftLock)}.
    */
   public void testUnlockItem() {
       getEntityAccessStrategy().unlockItem(KEY, new MockSoftLock());
   }

   /**
    * Test method for {@link TransactionalAccess#unlockRegion(org.hibernate.cache.access.SoftLock)}.
    */
   public void testUnlockRegion() {
       getEntityAccessStrategy().unlockItem(KEY, new MockSoftLock());
   }

   /**
    * Test method for {@link TransactionalAccess#afterInsert(java.lang.Object, java.lang.Object, java.lang.Object)}.
    */
   public void testAfterInsert() {
       assertFalse("afterInsert always returns false", getEntityAccessStrategy().afterInsert(KEY, VALUE1, new Integer(1)));
   }

   /**
    * Test method for {@link TransactionalAccess#afterUpdate(java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object, org.hibernate.cache.access.SoftLock)}.
    */
   public void testAfterUpdate() {
       assertFalse("afterInsert always returns false", getEntityAccessStrategy().afterUpdate(KEY, VALUE2, new Integer(1), new Integer(2), new MockSoftLock()));
   }
   
   public static class MockSoftLock implements SoftLock {
       
   }


}
