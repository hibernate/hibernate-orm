/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.cache;

import java.util.function.Function;

/**
 * Contract for internal caches.
 * We call it "internal" Cache to disambiguate from the canonical entity 2LC, as that one is more commonly
 * used by users and therefore of public knowledge.
 * We highly prefer caches to be implemented by reputable caching libraries, this reduces our maintenance complexity
 * (as maintaining a proper cache implementation is not at all trivial) and allows for people to experiment with
 * various algorithms, including state-of-the-art that we might not be familiar with.
 * For these reasons, we rely on this internal interface and encourage plugging in an external implementation;
 * at the time of writing this we'll have a legacy implementation for backwards compatibility reasons but the general
 * idea is to deprecate it and eventually require a third party implementation.
 */
public interface InternalCache<K, V> {

	/**
	 * @return An estimate of the number of values contained in the cache.
	 */
	int heldElementsEstimate();

	/**
	 * Attempt to read from the cache. Will return null on cache miss.
	 * It would typically be better to use {@link #computeIfAbsent(Object, Function)} instead.
	 */
	V get(K key);

	/**
	 * Stores a key/value pair into the cache. Storage is not guaranteed, as the implementation
	 * has liberty to cap internal storage or use various eviction strategies.
	 */
	void put(K key, V value);

	/**
	 * Attempts to clear the content of the cache. Note that in some cache implementations this
	 * is not a trivial operation and should not be used on a performance critical path.
	 * Also note that thorough cleanup is not guaranteed:
	 * in some implementations it's a "best effort" strategy, or could be ignored altogether.
	 * Essentially it's useful as a hint that the client will no longer likely need to stored entries,
	 * so to save memory, if possible.
	 */
	void clear();

	/**
	 * This should be the preferred main strategy to benefit from the cache: it allows to implement
	 * the general pattern of "try to read, or produce a value and then cache it" but avoiding
	 * efficiency issues that would be caused by accessing the cache multiple times, not least
	 * potentially a cache stampede, and concurrent need for generating the same value.
	 * @param mappingFunction This function will be invoked to produce the value, and store it,
	 * if a matching existing value couldn't be loaded from the cache.
	 * @return Either the existing value, or the return from the provided function.
	 */
	V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);
}
