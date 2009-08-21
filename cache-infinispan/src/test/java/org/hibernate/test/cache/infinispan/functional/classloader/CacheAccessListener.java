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

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;

@Listener
public class CacheAccessListener {
//   HashSet<Fqn<String>> modified = new HashSet<Fqn<String>>();
//   HashSet<Fqn<String>> accessed = new HashSet<Fqn<String>>();
   HashSet modified = new HashSet();
   HashSet accessed = new HashSet();

   public void clear() {
      modified.clear();
      accessed.clear();
   }

   @CacheEntryModified
   public void nodeModified(CacheEntryModifiedEvent event) {
      if (!event.isPre()) {
         Object key = event.getKey();
         System.out.println("MyListener - Modified node " + key);
         modified.add(key);
      }
   }
   
   @CacheEntryCreated
   public void nodeCreated(CacheEntryCreatedEvent event) {
      if (!event.isPre()) {
         Object key = event.getKey();
         System.out.println("MyListener - Created node " + key);
         modified.add(key);
      }
   }

   @CacheEntryVisited
   public void nodeVisited(CacheEntryVisitedEvent event) {
      if (!event.isPre()) {
         Object key = event.getKey();
         System.out.println("MyListener - Visited node " + key);
         accessed.add(key);
      }
   }

   public boolean getSawRegionModification(Object key) {
      return getSawRegion(key, modified);
   }

   public boolean getSawRegionAccess(Object key) {
      return getSawRegion(key, accessed);
   }

   private boolean getSawRegion(Object key, Set sawEvents) {
      if (sawEvents.contains(key)) {
         sawEvents.remove(key);
         return true;
      }
      return false;
//      boolean saw = false;
//      for (Object key : sawEvents) {
//         
//      }
//      Fqn<String> fqn = Fqn.fromString(regionName);
//      for (Iterator<Fqn<String>> it = sawEvent.iterator(); it.hasNext();) {
//         Fqn<String> modified = (Fqn<String>) it.next();
//         if (modified.isChildOf(fqn)) {
//            it.remove();
//            saw = true;
//         }
//      }
//      return saw;
   }

}