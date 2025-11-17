/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.transform;

import java.util.Arrays;
import java.util.List;

import org.hibernate.query.TypedTupleTransformer;

/**
 * Transforms each result row from a tuple into a {@link List} whose elements are each tuple value
 *
 * @deprecated since {@link ResultTransformer} is deprecated
 */
@Deprecated
public class ToListResultTransformer implements ResultTransformer<List<Object>>, TypedTupleTransformer<List<Object>> {
	public static final ToListResultTransformer INSTANCE = new ToListResultTransformer();

	/**
	 * Disallow instantiation of ToListResultTransformer.
	 */
	private ToListResultTransformer() {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Class<List<Object>> getTransformedType() {
		return (Class) List.class;
	}

	@Override
	public List<Object> transformTuple(Object[] tuple, String[] aliases) {
		return Arrays.asList( tuple );
	}

	/**
	 * Serialization hook for ensuring singleton uniqueing.
	 *
	 * @return The singleton instance : {@link #INSTANCE}
	 */
	private Object readResolve() {
		return INSTANCE;
	}
}
