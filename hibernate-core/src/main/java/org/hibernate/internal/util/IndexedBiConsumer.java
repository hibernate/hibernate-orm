/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

/**
 * A consumer, like {@link java.util.function.BiConsumer}, accepting 2 values and an index.
 *
 * @see IndexedConsumer
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface IndexedBiConsumer<T, U> {
	void accept(int index, T t, U u);
}
