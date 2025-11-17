/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

import java.util.function.Function;

import org.hibernate.LockMode;

/**
 * A concurrent safe EnumMap&lt;LockMode&gt;, suitable to
 * lazily associate values to the enum keys.
 * This implementation favours fast read operations
 * and low memory consumption over other metrics.
 *
 * Specifically designed with specific use cases in mind:
 * do not overly reuse without good reasons.
 *
 * @param <V> the value type to be associated with each key
 */
public final class LockModeEnumMap<V> extends LazyIndexedMap<LockMode,V> {

	private static final int ENUM_DIMENSION = LockMode.values().length;

	public LockModeEnumMap() {
		super( ENUM_DIMENSION );
	}

	public V computeIfAbsent(LockMode key, Function<LockMode,V> valueGenerator) {
		return super.computeIfAbsent( key.ordinal(), key, valueGenerator );
	}

}
