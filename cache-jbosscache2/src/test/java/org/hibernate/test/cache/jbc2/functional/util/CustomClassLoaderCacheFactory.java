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

package org.hibernate.test.cache.jbc2.functional.util;

import org.jboss.cache.CacheImpl;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.config.Configuration;

/**
 * CacheFactory impl that allows us to register a desired default classloader
 * for deserializing RPCs.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public class CustomClassLoaderCacheFactory<K, V> extends DefaultCacheFactory<K, V>
{
   private ClassLoader customClassLoader;
   
   /**
    * Create a new CustomClassLoaderCacheFactory.
    */
   public CustomClassLoaderCacheFactory(ClassLoader customClassLoader)
   {
      super();
      this.customClassLoader = customClassLoader;
   }
   
   @Override
   protected void bootstrap(CacheImpl cache, CacheSPI spi, Configuration configuration)
   {
      super.bootstrap(cache, spi, configuration);
      
      // Replace the deployerClassLoader
      componentRegistry.registerComponent("deployerClassLoader", customClassLoader, ClassLoader.class);
   }   
  
}
