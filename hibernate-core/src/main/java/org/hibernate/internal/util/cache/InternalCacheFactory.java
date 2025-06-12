/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.cache;

import org.hibernate.service.Service;

/**
 * Internal components can use this factory to create an efficient cache for internal purposes.
 * The implementation is pluggable, therefore the exact eviction and sizing semantics are unspecified
 * and responsibility of the implementation.
 */
public interface InternalCacheFactory extends Service {

	<K,V> InternalCache<K,V> createInternalCache(int intendedApproximateSize);

}
