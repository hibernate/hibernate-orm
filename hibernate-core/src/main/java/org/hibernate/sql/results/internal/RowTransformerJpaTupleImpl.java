/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import jakarta.persistence.Tuple;

import org.hibernate.sql.results.spi.RowTransformer;

/**
 * RowTransformer generating a JPA {@link Tuple}
 *
 * @author Steve Ebersole
 */
public class RowTransformerJpaTupleImpl implements RowTransformer<Tuple> {
	private final TupleMetadata tupleMetadata;

	public RowTransformerJpaTupleImpl(TupleMetadata tupleMetadata) {
		this.tupleMetadata = tupleMetadata;
	}

	@Override
	public Tuple transformRow(Object[] row) {
		return new TupleImpl( tupleMetadata, row );
	}

	@Override
	public int determineNumberOfResultElements(int rawElementCount) {
		return 1;
	}
}
