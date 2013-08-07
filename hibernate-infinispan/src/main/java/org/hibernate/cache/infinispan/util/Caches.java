/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
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

import java.util.concurrent.Callable;

import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;
import org.infinispan.remoting.rpc.RpcManager;

/**
 * Helper for dealing with Infinispan cache instances.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
public class Caches {

   private Caches() {
      // Suppresses default constructor, ensuring non-instantiability.
   }

   public static <T> T withinTx(AdvancedCache cache,
         Callable<T> c) throws Exception {
      // Retrieve transaction manager
      return withinTx(cache.getTransactionManager(), c);
   }

	public static <T> T withinTx(TransactionManager tm, Callable<T> c) throws Exception {
		if ( tm == null ) {
			try {
				return c.call();
			}
			catch ( Exception e ) {
				throw e;
			}
		}
		else {
			tm.begin();
			try {
				return c.call();
			}
			catch ( Exception e ) {
				tm.setRollbackOnly();
				throw e;
			}
			finally {
				if ( tm.getStatus() == Status.STATUS_ACTIVE )
					tm.commit();
				else
					tm.rollback();
			}
		}
	}

   public static AdvancedCache localCache(AdvancedCache cache) {
      return cache.withFlags(Flag.CACHE_MODE_LOCAL);
   }

   public static AdvancedCache ignoreReturnValuesCache(AdvancedCache cache) {
      return cache.withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP);
   }

   public static AdvancedCache ignoreReturnValuesCache(
         AdvancedCache cache, Flag extraFlag) {
      return cache.withFlags(
            Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP, extraFlag);
   }

   public static AdvancedCache asyncWriteCache(AdvancedCache cache,
         Flag extraFlag) {
      return cache.withFlags(
            Flag.SKIP_CACHE_LOAD,
            Flag.SKIP_REMOTE_LOOKUP,
            Flag.FORCE_ASYNCHRONOUS,
            extraFlag);
   }

   public static AdvancedCache failSilentWriteCache(AdvancedCache cache) {
      return cache.withFlags(
            Flag.FAIL_SILENTLY,
            Flag.ZERO_LOCK_ACQUISITION_TIMEOUT,
            Flag.SKIP_CACHE_LOAD,
            Flag.SKIP_REMOTE_LOOKUP);
   }

   public static AdvancedCache failSilentWriteCache(AdvancedCache cache,
         Flag extraFlag) {
      return cache.withFlags(
            Flag.FAIL_SILENTLY,
            Flag.ZERO_LOCK_ACQUISITION_TIMEOUT,
            Flag.SKIP_CACHE_LOAD,
            Flag.SKIP_REMOTE_LOOKUP,
            extraFlag);
   }

   public static AdvancedCache failSilentReadCache(AdvancedCache cache) {
      return cache.withFlags(
            Flag.FAIL_SILENTLY,
            Flag.ZERO_LOCK_ACQUISITION_TIMEOUT);
   }

   public static void broadcastEvictAll(AdvancedCache cache) {
      RpcManager rpcManager = cache.getRpcManager();
      if (rpcManager != null) {
         // Only broadcast evict all if it's clustered
         CacheCommandInitializer factory = cache.getComponentRegistry()
               .getComponent(CacheCommandInitializer.class);
         boolean isSync = isSynchronousCache(cache);

         EvictAllCommand cmd = factory.buildEvictAllCommand(cache.getName());
         rpcManager.broadcastRpcCommand(cmd, isSync);
      }
   }

   public static boolean isInvalidationCache(AdvancedCache cache) {
      return cache.getCacheConfiguration()
            .clustering().cacheMode().isInvalidation();
   }

   public static boolean isSynchronousCache(AdvancedCache cache) {
      return cache.getCacheConfiguration()
            .clustering().cacheMode().isSynchronous();
   }

   public static boolean isClustered(AdvancedCache cache) {
      return cache.getCacheConfiguration()
            .clustering().cacheMode().isClustered();
   }

}
