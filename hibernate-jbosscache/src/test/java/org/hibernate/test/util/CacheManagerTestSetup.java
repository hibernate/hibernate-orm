/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Brian Stansberry
 */

package org.hibernate.test.util;

import java.util.concurrent.atomic.AtomicReference;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;
import org.jboss.cache.CacheManager;
import org.jboss.cache.CacheManagerImpl;
import org.jgroups.ChannelFactory;
import org.jgroups.JChannelFactory;

/**
 * A TestSetup that starts a CacheManager in setUp() and stops it in tearDown().
 * AtomicReference passed to the constructor can be used by the test to
 * access the CacheManager.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class CacheManagerTestSetup extends TestSetup
{
   public static final String DEF_CACHE_FACTORY_RESOURCE = MultiplexingCacheInstanceManager.DEF_CACHE_FACTORY_RESOURCE;
   public static final String DEF_JGROUPS_RESOURCE = MultiplexingCacheInstanceManager.DEF_JGROUPS_RESOURCE;
   
   private final String jbcConfig;
   private final String jgConfig;
   private final AtomicReference<CacheManager> cacheManagerRef;
   private ChannelFactory channelFactory;
   
   public CacheManagerTestSetup(Test test, AtomicReference<CacheManager> cacheManagerRef)
   {
      this(test, DEF_CACHE_FACTORY_RESOURCE, DEF_JGROUPS_RESOURCE, cacheManagerRef);
   }
   
   public CacheManagerTestSetup(Test test, String jbcConfig, String jgConfig, AtomicReference<CacheManager> cacheManagerRef)
   {
      super(test);
      this.jbcConfig = jbcConfig;
      this.jgConfig  = jgConfig;
      this.cacheManagerRef = cacheManagerRef;
   }
   
   public CacheManagerTestSetup(Test test, String jbcConfig, ChannelFactory channelFactory, AtomicReference<CacheManager> cacheManagerRef)
   {
      super(test);
      this.jbcConfig = jbcConfig;
      this.jgConfig  = null;
      this.cacheManagerRef = cacheManagerRef;
      this.channelFactory = channelFactory;
   }

   @Override
   protected void setUp() throws Exception
   {      
      super.setUp();
      
      if (this.channelFactory == null)
      {
         this.channelFactory = new JChannelFactory();
         this.channelFactory.setMultiplexerConfig(this.jgConfig);
      }
      
      CacheManagerImpl jbcFactory = new CacheManagerImpl(this.jbcConfig, this.channelFactory);
      this.cacheManagerRef.set(jbcFactory);
      jbcFactory.start();
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      this.channelFactory = null;
      
      CacheManager jbcFactory = this.cacheManagerRef.get();
      this.cacheManagerRef.set(null);
      ((CacheManagerImpl) jbcFactory).stop();
   }
   
}