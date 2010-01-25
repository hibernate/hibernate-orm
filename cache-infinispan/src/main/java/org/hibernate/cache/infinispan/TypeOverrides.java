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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

   private final Set<String> overridden = new HashSet<String>();

   private String cacheName;

   private EvictionStrategy evictionStrategy;

   private long evictionWakeUpInterval;

   private int evictionMaxEntries;

   private long expirationLifespan;

   private long expirationMaxIdle;

   private boolean isExposeStatistics;

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
      markAsOverriden("evictionStrategy");
      this.evictionStrategy = EvictionStrategy.valueOf(uc(evictionStrategy));
   }

   public long getEvictionWakeUpInterval() {
      return evictionWakeUpInterval;
   }

   public void setEvictionWakeUpInterval(long evictionWakeUpInterval) {
      markAsOverriden("evictionWakeUpInterval");
      this.evictionWakeUpInterval = evictionWakeUpInterval;
   }

   public int getEvictionMaxEntries() {
      return evictionMaxEntries;
   }

   public void setEvictionMaxEntries(int evictionMaxEntries) {
      markAsOverriden("evictionMaxEntries");
      this.evictionMaxEntries = evictionMaxEntries;
   }

   public long getExpirationLifespan() {
      return expirationLifespan;
   }

   public void setExpirationLifespan(long expirationLifespan) {
      markAsOverriden("expirationLifespan");
      this.expirationLifespan = expirationLifespan;
   }

   public long getExpirationMaxIdle() {
      return expirationMaxIdle;
   }

   public void setExpirationMaxIdle(long expirationMaxIdle) {
      markAsOverriden("expirationMaxIdle");
      this.expirationMaxIdle = expirationMaxIdle;
   }

   public boolean isExposeStatistics() {
      return isExposeStatistics;
   }

   public void setExposeStatistics(boolean isExposeStatistics) {
      markAsOverriden("isExposeStatistics");
      this.isExposeStatistics = isExposeStatistics;
   }

   public Configuration createInfinispanConfiguration() {
      Configuration cacheCfg = new Configuration();
      if (overridden.contains("evictionStrategy")) cacheCfg.setEvictionStrategy(evictionStrategy);
      if (overridden.contains("evictionWakeUpInterval")) cacheCfg.setEvictionWakeUpInterval(evictionWakeUpInterval);
      if (overridden.contains("evictionMaxEntries")) cacheCfg.setEvictionMaxEntries(evictionMaxEntries);
      if (overridden.contains("expirationLifespan")) cacheCfg.setExpirationLifespan(expirationLifespan);
      if (overridden.contains("expirationMaxIdle")) cacheCfg.setExpirationMaxIdle(expirationMaxIdle);
      if (overridden.contains("isExposeStatistics")) cacheCfg.setExposeJmxStatistics(isExposeStatistics);
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

   private void markAsOverriden(String fieldName) {
      overridden.add(fieldName);
   }
}
