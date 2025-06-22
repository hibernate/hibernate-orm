/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * Verifies that the first object in each row is
 * assignable to the query result type.
 * <p>
 * Should only be used when the query has exactly
 * one item in its selection list.
 *
 * @author Gavin King
 */
public class RowTransformerCheckingImpl<R> implements RowTransformer<R> {

	private final Class<R> type;

	public RowTransformerCheckingImpl(Class<R> type) {
		this.type = type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public R transformRow(Object[] row) {
		final Object result = row[0];
		if ( result == null || type.isInstance( result ) ) {
			return (R) result;
		}
		else {
			throw new QueryTypeMismatchException( "Result type is '" + type.getSimpleName()
					+ "' but the query returned a '" + result.getClass().getSimpleName() + "'" );
		}
	}

	@Override
	public int determineNumberOfResultElements(int rawElementCount) {
		return 1;
	}
}
