/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

/**
 * A consumer, like {@link java.util.function.Consumer}, accepting a value and its index.
 *
 * @see IndexedBiConsumer
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface IndexedConsumer<T> {
	void accept(int index, T t);
}
