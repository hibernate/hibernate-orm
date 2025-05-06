/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

/**
 * A consumer, like {@link java.util.function.BiConsumer}, accepting a key and a value (Map entry e.g.)
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface KeyedConsumer<K,V> {
	void accept(K key, V value);
}
