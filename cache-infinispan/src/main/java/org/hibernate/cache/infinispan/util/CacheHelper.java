/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
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
package org.hibernate.cache.infinispan.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Helper for dealing with Infinisan cache instances.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class CacheHelper {

   private static final Log log = LogFactory.getLog(CacheHelper.class);

   /**
    * Disallow external instantiation of CacheHelper.
    */
   private CacheHelper() {
   }

   public static void initInternalEvict(CacheAdapter cacheAdapter, AddressAdapter member) {
      EvictAll eKey = new EvictAll(member == null ? NoAddress.INSTANCE : member);
      cacheAdapter.withFlags(FlagAdapter.CACHE_MODE_LOCAL).put(eKey, Internal.INIT);
   }

   public static void sendEvictAllNotification(CacheAdapter cacheAdapter, AddressAdapter member) {
      EvictAll eKey = new EvictAll(member == null ? NoAddress.INSTANCE : member);
      cacheAdapter.put(eKey, Internal.EVICT);
   }

   public static boolean isEvictAllNotification(Object key) {
      return key instanceof EvictAll;
   }

   public static boolean containsEvictAllNotification(Set keySet, AddressAdapter member) {
      EvictAll eKey = new EvictAll(member == null ? NoAddress.INSTANCE : member);
      return keySet.contains(eKey);
   }

   public static boolean isEvictAllNotification(Object key, Object value) {
      return key instanceof EvictAll && value == Internal.EVICT;
   }

   private static class EvictAll implements Externalizable {
      AddressAdapter member;

      EvictAll(AddressAdapter member) {
         this.member = member;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this)
            return true;
         if (!(obj instanceof EvictAll))
            return false;
         EvictAll ek = (EvictAll) obj;
         return ek.member.equals(member);
      }

      @Override
      public int hashCode() {
         int result = 17;
         result = 31 * result + member.hashCode();
         return result;
      }

      public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
         member = (AddressAdapter) in.readObject();
      }

      public void writeExternal(ObjectOutput out) throws IOException {
         out.writeObject(member);
      }
   }

   private enum NoAddress implements AddressAdapter {
      INSTANCE;
   }

   private enum Internal { 
      INIT, EVICT;
   }

}
