/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.InstanceIdentity;

/**
 * Utility collection that takes advantage of {@link InstanceIdentity}'s identifier to store objects.
 * The store is based on {@link AbstractPagedArray} and it stores element using their instance-id
 * as index.
 * <p>
 * Both keys and values are stored in this array, requiring very few allocations to keep track of the pair.
 * The downside to this is we cannot easily access the key, especially if asking for a specific type, since
 * that would cause type-pollution issues at the call site that would degrade performance.
 *
 * @param <V> the type of values contained in this store
 */
public class InstanceIdentityStore<V> extends AbstractPagedArray<Object> {
	/**
	 * Utility to derive the key index from an instance-id, keys are stored in every 2 positions in the array
	 *
	 * @param instanceId the instance identifier
	 * @return the index of the corresponding key instance in the array
	 */
	private static int toKeyIndex(int instanceId) {
		return instanceId * 2;
	}

	/**
	 * Returns the value to which the specified instance id is mapped, or {@code null} if this store
	 * contains no mapping for the instance id.
	 *
	 * @param instanceId the instance id whose associated value is to be returned
	 * @param key key instance to double-check instance equality
	 * @return the value to which the specified instance id is mapped,
	 * or {@code null} if this map contains no mapping for the instance id
	 * @implNote This method accesses the backing array with the provided instance id, but performs an instance
	 * equality check ({@code ==}) with the provided key to ensure it corresponds to the mapped one
	 */
	public @Nullable V get(int instanceId, Object key) {
		final int keyIndex = toKeyIndex( instanceId );
		final Page<Object> page = getPage( keyIndex );
		if ( page != null ) {
			final int offset = toPageOffset( keyIndex );
			final Object k = page.get( offset );
			if ( k == key ) {
				//noinspection unchecked
				return (V) page.get( offset + 1 );
			}
		}
		return null;
	}

	/**
	 * Associates the specified value with the specified key in this store (optional operation). If the store
	 * previously contained a mapping for the key, the old value is replaced by the specified value.
	 *
	 * @param key key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 */
	public <K extends InstanceIdentity> void put(K key, V value) {
		if ( key == null ) {
			throw new NullPointerException( "This store does not support null keys" );
		}

		final int instanceId = key.$$_hibernate_getInstanceId();
		final int keyIndex = toKeyIndex( instanceId );
		final Page<Object> page = getOrCreateEntryPage( keyIndex );
		final int pageOffset = toPageOffset( keyIndex );
		page.set( pageOffset, key );
		page.set( pageOffset + 1, value );
	}

	/**
	 * Removes the mapping for an instance id from this store if it is present (optional operation).
	 *
	 * @param instanceId the instance id whose associated value is to be returned
	 * @param key key instance to double-check instance equality
	 * @implNote This method accesses the backing array with the provided instance id, but performs an instance
	 * equality check ({@code ==}) with the provided key to ensure it corresponds to the mapped one
	 */
	public void remove(int instanceId, Object key) {
		final int keyIndex = toKeyIndex( instanceId );
		final Page<Object> page = getPage( keyIndex );
		if ( page != null ) {
			final int pageOffset = toPageOffset( keyIndex );
			Object k = page.set( pageOffset, null );
			// Check that the provided instance really matches with the key contained in the store
			if ( k == key ) {
				page.set( pageOffset + 1, null );
			}
			else {
				// If it doesn't, reset the array value to the old key
				page.set( pageOffset, k );
			}
		}
	}
}
