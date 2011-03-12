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

package org.hibernate.test.cache.jbc.functional.util;

import org.jboss.cache.Cache;
import org.jboss.cache.CacheManagerImpl;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.config.Configuration;
import org.jgroups.ChannelFactory;

/**
 * CacheManager implementation that lets us set a default ClassLoader
 * on the created cache. 
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class CustomClassLoaderCacheManager extends CacheManagerImpl
{
   private final ClassLoader defaultClassLoader;
   
   /**
    * Create a new CustomClassLoaderCacheManager.
    * 
    * @param configFileName
    * @param factory
    */
   public CustomClassLoaderCacheManager(String configFileName, 
                                        ChannelFactory factory,
                                        ClassLoader defaultClassLoader)
   {
      super(configFileName, factory);
      this.defaultClassLoader = defaultClassLoader;
   }

   @Override
   protected Cache<Object, Object> createCache(Configuration config)
   {
      DefaultCacheFactory<Object, Object> factory = new DefaultCacheFactory<Object, Object>();
      factory.setDefaultClassLoader(defaultClassLoader);
      return factory.createCache(config, false);
   }
   
   

}
