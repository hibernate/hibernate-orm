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
package org.hibernate.cache.infinispan;

import java.util.Locale;

import org.hibernate.cache.CacheException;
import org.infinispan.config.Configuration;
import org.infinispan.eviction.EvictionStrategy;

/**
 * This class represents Infinispan cache parameters that can be configured via hibernate configuration properties 
 * for either general entity/collection/query/timestamp data type caches and overrides for individual entity or 
 * collection caches. Configuration these properties override previously defined properties in XML file.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class TypeOverrides {
   
   private String cacheName;
   
   private EvictionStrategy evictionStrategy;
   
   private long evictionWakeUpInterval = Long.MIN_VALUE;
   
   private int evictionMaxEntries = Integer.MIN_VALUE;
   
   private long expirationLifespan = Long.MIN_VALUE;
   
   private long expirationMaxIdle = Long.MIN_VALUE;

   public String getCacheName() {
      return cacheName;
   }

   public void setCacheName(String cacheName) {
      this.cacheName = cacheName;
   }

   public EvictionStrategy getEvictionStrategy() {
      return evictionStrategy;
   }

   public void setEvictionStrategy(String evictionStrategy) {
      this.evictionStrategy = EvictionStrategy.valueOf(uc(evictionStrategy));
   }

   public long getEvictionWakeUpInterval() {
      return evictionWakeUpInterval;
   }

   public void setEvictionWakeUpInterval(long evictionWakeUpInterval) {
      this.evictionWakeUpInterval = evictionWakeUpInterval;
   }

   public int getEvictionMaxEntries() {
      return evictionMaxEntries;
   }

   public void setEvictionMaxEntries(int evictionMaxEntries) {
      this.evictionMaxEntries = evictionMaxEntries;
   }

   public long getExpirationLifespan() {
      return expirationLifespan;
   }

   public void setExpirationLifespan(long expirationLifespan) {
      this.expirationLifespan = expirationLifespan;
   }

   public long getExpirationMaxIdle() {
      return expirationMaxIdle;
   }

   public void setExpirationMaxIdle(long expirationMaxIdle) {
      this.expirationMaxIdle = expirationMaxIdle;
   }
   
//   public boolean isConvertedToInfinispanConfiguration() {
//      return convertedToInfinispanConfiguration;
//   }
   
   public Configuration createInfinispanConfiguration() {
      Configuration cacheCfg = new Configuration();
      // If eviction strategy is different from null, an override has been defined
      if (evictionStrategy != null) cacheCfg.setEvictionStrategy(evictionStrategy);
      // If eviction wake up interval is different from min value, an override has been defined
      // Checking for -1 might not be enough because user might have defined -1 in the config.
      // Same applies to other configuration options.
      if (evictionWakeUpInterval != Long.MIN_VALUE) cacheCfg.setEvictionWakeUpInterval(evictionWakeUpInterval);
      if (evictionMaxEntries != Integer.MIN_VALUE) cacheCfg.setEvictionMaxEntries(evictionMaxEntries);
      if (expirationLifespan != Long.MIN_VALUE) cacheCfg.setExpirationLifespan(expirationLifespan); 
      if (expirationMaxIdle != Long.MIN_VALUE) cacheCfg.setExpirationMaxIdle(expirationMaxIdle);
//      convertedToInfinispanConfiguration = true;
      return cacheCfg;
   }
   
   public void validateInfinispanConfiguration(Configuration configuration) throws CacheException {
      // no-op
   }
   
   @Override
   public String toString() {
      return new StringBuilder().append(getClass().getSimpleName()).append('{')
         .append("cache=").append(cacheName)
         .append(", strategy=").append(evictionStrategy)
         .append(", wakeUpInterval=").append(evictionWakeUpInterval)
         .append(", maxEntries=").append(evictionMaxEntries)
         .append(", lifespan=").append(expirationLifespan)
         .append(", maxIdle=").append(expirationMaxIdle)
         .append('}').toString();
   }
   
   private String uc(String s) {
      return s == null ? null : s.toUpperCase(Locale.ENGLISH);
   }
}
