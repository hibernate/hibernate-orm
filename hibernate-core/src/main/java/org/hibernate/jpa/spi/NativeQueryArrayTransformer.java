/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.query.TupleTransformer;

/**
 * A {@link TupleTransformer} for handling {@code Object[]} results from native queries.
 *
 * @since 7.0
 */
public class NativeQueryArrayTransformer implements TupleTransformer<Object[]> {

	public static final NativeQueryArrayTransformer INSTANCE = new NativeQueryArrayTransformer();

	@Override
	@Nonnull
	public Object[] transformTuple(@Nonnull Object[] tuple, @Nonnull String[] aliases) {
		return tuple;
	}
}
