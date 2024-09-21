/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
