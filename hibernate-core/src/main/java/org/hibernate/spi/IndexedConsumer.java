/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * A consumer, like {@link java.util.function.Consumer}, accepting a value and its index.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
@FunctionalInterface
@SPI({ USE, IMPLEMENT })
public interface IndexedConsumer<T> {
	void accept(int index, T t);
}
