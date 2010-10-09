/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat, Inc. and/or it's affiliates or third-party contributors as
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
package org.hibernate.test.cache.infinispan.functional.classloader;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.cache.infinispan.util.CacheHelper;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Listener
public class CacheAccessListener {
   protected final Logger log = LoggerFactory.getLogger(getClass());
   
   HashSet modified = new HashSet();
   HashSet accessed = new HashSet();

   public void clear() {
      modified.clear();
      accessed.clear();
   }

   @CacheEntryModified
   public void nodeModified(CacheEntryModifiedEvent event) {
      if (!event.isPre() && !CacheHelper.isEvictAllNotification(event.getKey())) {
         Object key = event.getKey();
         log.info("Modified node " + key);
         modified.add(key.toString());
      }
   }
   
   @CacheEntryCreated
   public void nodeCreated(CacheEntryCreatedEvent event) {
      if (!event.isPre() && !CacheHelper.isEvictAllNotification(event.getKey())) {
         Object key = event.getKey();
         log.info("Created node " + key);
         modified.add(key.toString());
      }
   }

   @CacheEntryVisited
   public void nodeVisited(CacheEntryVisitedEvent event) {
      if (!event.isPre() && !CacheHelper.isEvictAllNotification(event.getKey())) {
         Object key = event.getKey();
         log.info("Visited node " + key);
         accessed.add(key.toString());
      }
   }

   public boolean getSawRegionModification(Object key) {
      return getSawRegion(key, modified);
   }
   
   public int getSawRegionModificationCount() {
      return modified.size();
   }
   
   public void clearSawRegionModification() {
      modified.clear();
   }

   public boolean getSawRegionAccess(Object key) {
      return getSawRegion(key, accessed);
   }

   public int getSawRegionAccessCount() {
      return accessed.size();
   }
   
   public void clearSawRegionAccess() {
      accessed.clear();
   }

   private boolean getSawRegion(Object key, Set sawEvents) {
      if (sawEvents.contains(key)) {
         sawEvents.remove(key);
         return true;
      }
      return false;
  }

}