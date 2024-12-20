/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.collections;

public interface ReadOnlyMap<K, V> {

	//To help saving memory
	public static final ReadOnlyMap EMPTY = new EmptyReadOnlyMap();

	/**
	 * The main operation.
	 * @param key
	 * @return the corresponding object, or null if there is no association with any entry.
	 */
	V get(K key);

	/**
	 * Some implementations might hold on to references,
	 * which could be just heavy or potentially harmful,
	 * such as ClassLoader leaks: allow for proper cleanup.
	 */
	void dispose();

}
