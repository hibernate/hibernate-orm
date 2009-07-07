/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.cache.jbc.functional.classloader;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jboss.cache.Fqn;
import org.jboss.cache.notifications.annotation.CacheListener;
import org.jboss.cache.notifications.annotation.NodeCreated;
import org.jboss.cache.notifications.annotation.NodeModified;
import org.jboss.cache.notifications.annotation.NodeVisited;
import org.jboss.cache.notifications.event.NodeCreatedEvent;
import org.jboss.cache.notifications.event.NodeModifiedEvent;
import org.jboss.cache.notifications.event.NodeVisitedEvent;

@CacheListener
public class CacheAccessListener
{
   HashSet<Fqn<String>> modified = new HashSet<Fqn<String>>(); 
   HashSet<Fqn<String>> accessed = new HashSet<Fqn<String>>();
   
   public void clear()
   {
      modified.clear();
      accessed.clear();
   }
   
   @NodeModified
   public void nodeModified(NodeModifiedEvent event)
   {
      if (!event.isPre())
      {
         Fqn<String> fqn = event.getFqn();
         System.out.println("MyListener - Modified node " + fqn.toString());
         modified.add(fqn);
      }
   }

   @NodeCreated
   public void nodeCreated(NodeCreatedEvent event)
   {   
      if (!event.isPre())
      {
         Fqn<String> fqn = event.getFqn();
         System.out.println("MyListener - Created node " + fqn.toString());
         modified.add(fqn);
      }
   }   

   @NodeVisited
   public void nodeVisited(NodeVisitedEvent event)
   {   
      if (!event.isPre())
      {
         Fqn<String> fqn = event.getFqn();
         System.out.println("MyListener - Visited node " + fqn.toString());
         accessed.add(fqn); 
      }
   }    
   
   public boolean getSawRegionModification(String regionName)
   {
      return getSawRegion(regionName, modified);
   }
   
   public boolean getSawRegionAccess(String regionName)
   {
      return getSawRegion(regionName, accessed);
   }
   
   private boolean getSawRegion(String regionName, Set<Fqn<String>> sawEvent)
   {
      boolean saw = false;
      Fqn<String> fqn = Fqn.fromString(regionName);
      for (Iterator<Fqn<String>> it = sawEvent.iterator(); it.hasNext();)
      {
         Fqn<String> modified = (Fqn<String>) it.next();
         if (modified.isChildOf(fqn))
         {
            it.remove();
            saw = true;
         }
      }
   return saw;
      
   }
   
}