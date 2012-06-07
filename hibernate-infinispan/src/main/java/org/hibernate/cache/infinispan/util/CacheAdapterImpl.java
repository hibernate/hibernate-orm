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
import java.util.concurrent.Callable;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.util.concurrent.TimeoutException;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import org.hibernate.cache.CacheException;

/**
 * CacheAdapterImpl.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class CacheAdapterImpl implements CacheAdapter {
   private static final Log log = LogFactory.getLog(CacheAdapterImpl.class);

   private final AdvancedCache cache;
   private final CacheCommandInitializer cacheCmdInitializer;
   private final boolean isSync;

   private CacheAdapterImpl(AdvancedCache cache) {
      this.cache = cache;
      this.cacheCmdInitializer = cache.getComponentRegistry()
            .getComponent(CacheCommandInitializer.class);
      this.isSync = isSynchronous(cache.getConfiguration().getCacheMode());
   }

   public static CacheAdapter newInstance(AdvancedCache cache) {
      return new CacheAdapterImpl(cache);
   }

   public boolean isClusteredInvalidation() {
      return isClusteredInvalidation(cache.getConfiguration().getCacheMode());
   }

   public boolean isClusteredReplication() {
      return isClusteredReplication(cache.getConfiguration().getCacheMode());
   }

   public boolean isSynchronous() {
      return isSync;
   }

   public Set keySet() {
      return cache.keySet();
   }

   public CacheAdapter withFlags(FlagAdapter... flagAdapters) {
      Flag[] flags = FlagAdapter.toFlags(flagAdapters);
      return newInstance(cache.withFlags(flags));
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
         return getFailSilentCache().get(key);
      } catch (TimeoutException ignored) {
         // ignore it
         return null;
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   public void put(Object key, Object value) throws CacheException {
      try {
         // No previous value interest, so apply flags that avoid remote lookups.
         getSkipRemoteGetLoadCache().put(key, value);
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   public void putAllowingTimeout(Object key, Object value) throws CacheException {
      try {
         // No previous value interest, so apply flags that avoid remote lookups.
         getFailSilentCacheSkipRemotes().put(key, value);
      } catch (TimeoutException allowed) {
         // ignore it
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   public void putForExternalRead(Object key, Object value) throws CacheException {
      try {
         // No previous value interest, so apply flags that avoid remote lookups.
         getFailSilentCacheSkipRemotes().putForExternalRead(key, value);
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   public void remove(Object key) throws CacheException {
      try {
         // No previous value interest, so apply flags that avoid remote lookups.
         getSkipRemoteGetLoadCache().remove(key);
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

   public void stop() {
      if (log.isTraceEnabled())
         log.trace("Stop " + cache); 
      cache.stop();
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
      RpcManager rpc = cache.getRpcManager();
      if (rpc != null) {
         return AddressAdapterImpl.newInstance(rpc.getTransport().getAddress());
      }
      return null;
   }

   public List<AddressAdapter> getMembers() {
      RpcManager rpc = cache.getRpcManager();
      if (rpc != null) {
         return AddressAdapterImpl.toAddressAdapter(rpc.getTransport().getMembers());
      }
      return null;
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

   @Override
   public void broadcastEvictAll() {
      RpcManager rpcManager = cache.getRpcManager();
      if (rpcManager != null) {
         // Only broadcast evict all if it's clustered
         EvictAllCommand cmd = cacheCmdInitializer.buildEvictAllCommand(cache.getName());
         rpcManager.broadcastRpcCommand(cmd, isSync);
      }
   }

   @Override
   public <T> T withinTx(Callable<T> c) throws Exception {
      return CacheHelper.withinTx(cache.getTransactionManager(), c);
   }

   @Override
   public Cache getCache() {
      return cache;
   }

   private Cache getFailSilentCache() {
      return cache.withFlags(Flag.FAIL_SILENTLY);
   }

   private Cache getSkipRemoteGetLoadCache() {
      return cache.withFlags(
            Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP);
   }

   private Cache getFailSilentCacheSkipRemotes() {
      return cache.withFlags(
            Flag.FAIL_SILENTLY, Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP);
   }

}
