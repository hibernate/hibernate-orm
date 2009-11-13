/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of
 * individual contributors.
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
package org.hibernate.cache.infinispan.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.cache.CacheException;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.util.concurrent.TimeoutException;

/**
 * CacheAdapterImpl.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class CacheAdapterImpl implements CacheAdapter {

   private final Cache cache;

   private CacheAdapterImpl(Cache cache) {
      this.cache = cache;
   }

   public static CacheAdapter newInstance(Cache cache) {
      return new CacheAdapterImpl(cache);
   }

   public boolean isClusteredInvalidation() {
      return isClusteredInvalidation(cache.getConfiguration().getCacheMode());
   }

   public boolean isClusteredReplication() {
      return isClusteredReplication(cache.getConfiguration().getCacheMode());
   }

   public boolean isSynchronous() {
      return isSynchronous(cache.getConfiguration().getCacheMode());
   }

   public Set keySet() {
      return cache.keySet();
   }

   public CacheAdapter withFlags(FlagAdapter... flagAdapters) {
      Flag[] flags = FlagAdapter.toFlags(flagAdapters);
      return newInstance(cache.getAdvancedCache().withFlags(flags));
   }

   public Object get(Object key) throws CacheException {
      try {
         return cache.get(key);
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   public Object getAllowingTimeout(Object key) throws CacheException {
      try {
         return cache.get(key);
      } catch (TimeoutException ignored) {
         // ignore it
         return null;
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   public Object put(Object key, Object value) throws CacheException {
      try {
         return cache.put(key, value);
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   public Object putAllowingTimeout(Object key, Object value) throws CacheException {
      try {
         return cache.put(key, value);
      } catch (TimeoutException allowed) {
         // ignore it
         return null;
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   public void putForExternalRead(Object key, Object value) throws CacheException {
      try {
         cache.putForExternalRead(key, value);
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   public Object remove(Object key) throws CacheException {
      try {
         return cache.remove(key);
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   public void evict(Object key) throws CacheException {
      try {
         cache.evict(key);
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   public void clear() throws CacheException {
      try {
         cache.clear();
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   private static boolean isClusteredInvalidation(Configuration.CacheMode cacheMode) {
      return cacheMode == Configuration.CacheMode.INVALIDATION_ASYNC
               || cacheMode == Configuration.CacheMode.INVALIDATION_SYNC;
   }

   private static boolean isClusteredReplication(Configuration.CacheMode cacheMode) {
      return cacheMode == Configuration.CacheMode.REPL_ASYNC
               || cacheMode == Configuration.CacheMode.REPL_SYNC;
   }

   private static boolean isSynchronous(Configuration.CacheMode cacheMode) {
      return cacheMode == Configuration.CacheMode.REPL_SYNC
               || cacheMode == Configuration.CacheMode.INVALIDATION_SYNC
               || cacheMode == Configuration.CacheMode.DIST_SYNC;
   }

   public void addListener(Object listener) {
      cache.addListener(listener);
   }

   public AddressAdapter getAddress() {
      RpcManager rpc = cache.getAdvancedCache().getRpcManager();
      if (rpc != null) {
         return AddressAdapterImpl.newInstance(rpc.getTransport().getAddress());
      }
      return null;
   }

   public List<AddressAdapter> getMembers() {
      RpcManager rpc = cache.getAdvancedCache().getRpcManager();
      if (rpc != null) {
         return AddressAdapterImpl.toAddressAdapter(rpc.getTransport().getMembers());
      }
      return null;
   }

   public RpcManager getRpcManager() {
      return cache.getAdvancedCache().getRpcManager();
   }

   public int size() {
      return cache.size();
   }

   public Map toMap() {
      return cache;
   }

   public void removeListener(Object listener) {
      cache.removeListener(listener);
   }

   public boolean containsKey(Object key) {
      return cache.containsKey(key);
   }

   public Configuration getConfiguration() {
      return cache.getConfiguration();
   }

}
