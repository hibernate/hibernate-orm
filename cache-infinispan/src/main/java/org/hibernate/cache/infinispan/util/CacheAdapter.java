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
import org.infinispan.util.concurrent.TimeoutException;

/**
 * Infinispan cache abstraction.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public interface CacheAdapter {

   /**
    * Is this cache participating in a cluster with invalidation?
    * 
    * @return true if the cache is configured for synchronous/asynchronous invalidation; false otherwise.
    */
   boolean isClusteredInvalidation();

   /**
    * Is this cache participating in a cluster with replication?
    * 
    * @return true if the cache is configured for synchronous/asynchronous invalidation; false otherwise.
    */
   boolean isClusteredReplication();

   /**
    * Is this cache configured for synchronous communication?
    * 
    * @return true if the cache is configured for synchronous communication; false otherwise.
    */
   boolean isSynchronous();

   /**
    * Set of keys of this cache.
    * 
    * @return Set containing keys stored in this cache.
    */
   Set keySet();

   /** 
    * A builder-style method that adds flags to any cache API call.
    * 
    * @param flagAdapters a set of flags to apply.  See the {@link FlagAdapter} documentation.
    * @return a cache on which a real operation is to be invoked.
    */
   CacheAdapter withFlags(FlagAdapter... flagAdapters);

   /**
    * Method to check whether a certain key exists in this cache.
    * 
    * @param key key to look up.
    * @return true if key is present, false otherwise.
    */
   boolean containsKey(Object key);

   /**
    * Performs an <code>get(Object)</code> on the cache, wrapping any exception in a {@link CacheException}.
    * 
    * @param key key to retrieve
    * @throws CacheException
    */
   Object get(Object key) throws CacheException;

   /**
    * Performs an <code>get(Object)</code> on the cache ignoring any {@link TimeoutException} 
    * and wrapping any other exception in a {@link CacheException}.
    * 
    * @param key key to retrieve
    * @throws CacheException
    */
   Object getAllowingTimeout(Object key) throws CacheException;

   /**
    * Performs a <code>put(Object, Object)</code> on the cache, wrapping any exception in a {@link CacheException}.
    * 
    * @param key key whose value will be modified
    * @param value data to store in the cache entry
    * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> 
    *         if there was no mapping for <tt>key</tt>.
    * @throws CacheException
    */
   Object put(Object key, Object value) throws CacheException;

   /**
    * Performs a <code>put(Object, Object)</code> on the cache ignoring any {@link TimeoutException} 
    * and wrapping any exception in a {@link CacheException}.
    * 
    * @param key key whose value will be modified
    * @param value data to store in the cache entry
    * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> 
    *         if there was no mapping for <tt>key</tt>.
    * @throws CacheException
    */
   Object putAllowingTimeout(Object key, Object value) throws CacheException;

   /**
    * See {@link Cache#putForExternalRead(Object, Object)} for detailed documentation.
    * 
    * @param key key with which the specified value is to be associated.
    * @param value value to be associated with the specified key.
    * @throws CacheException
    */
   void putForExternalRead(Object key, Object value) throws CacheException;

   /**
    * Performs a <code>remove(Object)</code>, wrapping any exception in a {@link CacheException}.
    * 
    * @param key key to be removed
    * @return the previous value associated with <tt>key</tt>, or 
    *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
    * @throws CacheException
    */
   Object remove(Object key) throws CacheException;

   /**
    * Evict the given key from memory.
    * 
    * @param key to evict.
    */
   void evict(Object key) throws CacheException;

   /**
    * Clear the cache.
    * 
    * @throws CacheException
    */
   void clear() throws CacheException;

   /**
    * Add listener to this cache.
    * 
    * @param listener to be added to cache.
    */
   void addListener(Object listener);

   /**
    * Get local cluster address.
    * 
    * @return Address representing local address.
    */
   AddressAdapter getAddress();

   /**
    * Get cluster members.
    * 
    * @return List of cluster member Address instances
    */
   List<AddressAdapter> getMembers();

   /**
    * Size of cache.
    * 
    * @return number of cache entries.
    */
   int size();

   /**
    * This method returns a Map view of the cache.
    * 
    * @return Map view of cache.
    */
   Map toMap();

   /**
    * Remove listener from cache instance.
    * 
    * @param listener to be removed.
    */
   void removeListener(Object listener);

   /**
    * Get cache configuration.
    * 
    * @return Configuration instance associated with this cache.
    */
   Configuration getConfiguration();
}
