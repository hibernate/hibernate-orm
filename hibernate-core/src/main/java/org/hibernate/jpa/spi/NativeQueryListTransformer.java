/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.spi;

import org.hibernate.query.TupleTransformer;

import java.util.List;

/**
 * A {@link TupleTransformer} for handling {@link List} results from native queries.
 *
 * @since 6.3
 *
 * @author Gavin King
 */
public class NativeQueryListTransformer implements TupleTransformer<List<Object>> {

	public static final NativeQueryListTransformer INSTANCE = new NativeQueryListTransformer();

	@Override
	public List<Object> transformTuple(Object[] tuple, String[] aliases) {
		return List.of( tuple );
	}
}
