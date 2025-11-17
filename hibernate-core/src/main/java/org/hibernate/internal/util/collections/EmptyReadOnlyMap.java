/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

final class EmptyReadOnlyMap<K,V> implements ReadOnlyMap<K,V> {

	@Override
	public V get(K key) {
		return null;
	}

	@Override
	public void dispose() {
		//no-op
	}

}
