/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.spi;

import org.hibernate.query.TupleTransformer;

/**
 * A {@link TupleTransformer} for handling {@code Object[]} results from native queries.
 *
 * @since 7.0
 */
public class NativeQueryArrayTransformer implements TupleTransformer<Object[]> {

	public static final NativeQueryArrayTransformer INSTANCE = new NativeQueryArrayTransformer();

	@Override
	public Object[] transformTuple(Object[] tuple, String[] aliases) {
		return tuple;
	}
}
