/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.hibernate.cache.infinispan;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.RegionFactory;
import org.hibernate.util.NamingHelper;
import org.hibernate.util.PropertiesHelper;
import org.infinispan.manager.CacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A {@link RegionFactory} for <a href="http://www.jboss.org/infinispan">Infinispan</a>-backed cache
 * regions that finds its cache manager in JNDI rather than creating one itself.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class JndiInfinispanRegionFactory extends InfinispanRegionFactory {

   private static final Log log = LogFactory.getLog(JndiInfinispanRegionFactory.class);

   /**
    * Specifies the JNDI name under which the {@link CacheManager} to use is bound.
    * There is no default value -- the user must specify the property.
    */
   public static final String CACHE_MANAGER_RESOURCE_PROP = "hibernate.cache.infinispan.cachemanager";
   
   @Override
   protected CacheManager createCacheManager(Properties properties) throws CacheException {
      String name = PropertiesHelper.getString(CACHE_MANAGER_RESOURCE_PROP, properties, null);
      if (name == null)
         throw new CacheException("Configuration property " + CACHE_MANAGER_RESOURCE_PROP + " not set");
      return locateCacheManager(name, NamingHelper.getJndiProperties(properties));
   }

   private CacheManager locateCacheManager(String jndiNamespace, Properties jndiProperties) {
      Context ctx = null;
      try {
          ctx = new InitialContext(jndiProperties);
          return (CacheManager) ctx.lookup(jndiNamespace);
      } catch (NamingException ne) {
          String msg = "Unable to retrieve CacheManager from JNDI [" + jndiNamespace + "]";
          log.info(msg, ne);
          throw new CacheException( msg );
      } finally {
          if (ctx != null) {
              try {
                  ctx.close();
              } catch( NamingException ne ) {
                  log.info("Unable to release initial context", ne);
              }
          }
      }
  }

}
