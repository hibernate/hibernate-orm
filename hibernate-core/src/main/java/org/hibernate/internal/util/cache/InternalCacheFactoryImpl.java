/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.cache;

final class InternalCacheFactoryImpl implements InternalCacheFactory {

	@Override
	public <K, V> InternalCache<K, V> createInternalCache(int intendedApproximateSize) {
		return new LegacyInternalCacheImplementation<>( intendedApproximateSize );
	}
}
