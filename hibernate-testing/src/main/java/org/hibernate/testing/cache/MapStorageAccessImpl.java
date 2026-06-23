/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.cache;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.Internal;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * StorageAccess impl wrapping a simple data Map (ConcurrentMap)
 *
 * @author Steve Ebersole
 */
public class MapStorageAccessImpl implements DomainDataStorageAccess {
	private ConcurrentMap<Object,Object> data;

	@Internal
	@Nullable
	public Object getFromData(@Nonnull Object key) {
		return data == null ? null : data.get( key );
	}

	@Override
	public boolean contains(@Nonnull Object key) {
		return data != null && data.containsKey( key );
	}

	@Override
	@Nullable
	public Object getFromCache(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		return getFromData( key );
	}

	@Override
	public void putIntoCache(
			@Nonnull Object key,
			@Nonnull Object value,
			@Nonnull SharedSessionContractImplementor session) {
		getOrMakeDataMap().put( key, value );
	}

	@Nonnull
	protected ConcurrentMap<Object,Object> getOrMakeDataMap() {
		if ( data == null ) {
			data = new ConcurrentHashMap<>();
		}
		return data;
	}

	@Override
	public void removeFromCache(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		if ( data == null ) {
			return;
		}

		data.remove( key );
	}

	@Override
	public void clearCache(@Nonnull SharedSessionContractImplementor session) {
		if ( data == null ) {
			return;
		}

		data.clear();
	}

	@Override
	public void evictData() {
		if ( data != null ) {
			data.clear();
		}
	}

	@Override
	public void evictData(@Nonnull Object key) {
		if ( data != null ) {
			data.remove( key );
		}
	}

	@Override
	public void release() {
		if ( data != null ) {
			data.clear();
			data = null;
		}
	}
}
