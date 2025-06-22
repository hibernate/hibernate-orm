/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import org.hibernate.query.TupleTransformer;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * An adapter for treating a {@link TupleTransformer} as a {@link RowTransformer}
 *
 * @author Steve Ebersole
 */
public class RowTransformerTupleTransformerAdapter<T> implements RowTransformer<T> {
	private final String[] aliases;
	private final TupleTransformer<T> tupleTransformer;

	public RowTransformerTupleTransformerAdapter(String[] aliases, TupleTransformer<T> tupleTransformer) {
		this.aliases = aliases;
		this.tupleTransformer = tupleTransformer;
	}

	@Override
	public T transformRow(Object[] row) {
		assert aliases == null || row.length == aliases.length;
		return tupleTransformer.transformTuple( row, aliases );
	}
}
