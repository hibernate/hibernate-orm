/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import static org.hibernate.internal.util.type.PrimitiveWrappers.cast;
import static org.hibernate.internal.util.type.PrimitiveWrappers.isInstance;

/**
 * Implementation of the JPA Tuple contract
 *
 * @author Steve Ebersole
 */
public class TupleImpl implements Tuple {
	private final TupleMetadata tupleMetadata;
	private final Object[] row;

	public TupleImpl(TupleMetadata tupleMetadata, Object[] row) {
		this.tupleMetadata = tupleMetadata;
		this.row = row;
	}

	@Override
	public <X> X get(TupleElement<X> tupleElement) {
		final Integer index = tupleMetadata.get( tupleElement );
		if ( index == null ) {
			throw new IllegalArgumentException(
					"Requested tuple element did not correspond to element in the result tuple"
			);
		}
		// index should be "in range" by nature of size check in ctor
		return cast( tupleElement.getJavaType(), row[index] );
	}

	@Override
	public <X> X get(String alias, Class<X> type) {
		final Object untyped = get( alias );
		if ( untyped != null ) {
			if ( !isInstance( type, untyped ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Requested tuple value [alias=%s, value=%s] cannot be assigned to requested type [%s]",
								alias,
								untyped,
								type.getName()
						)
				);
			}
		}
		return cast( type, untyped );
	}

	@Override
	public Object get(String alias) {
		final Integer index = tupleMetadata.get( alias );
		if ( index == null ) {
			throw new IllegalArgumentException(
					"Given alias [" + alias + "] did not correspond to an element in the result tuple"
			);
		}
		// index should be "in range" by nature of size check in ctor
		return row[index];
	}

	@Override
	public <X> X get(int i, Class<X> type) {
		final Object result = get( i );
		if ( result != null && !isInstance( type, result ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Requested tuple value [index=%s, realType=%s] cannot be assigned to requested type [%s]",
							i,
							result.getClass().getName(),
							type.getName()
					)
			);
		}
		return cast( type, result );
	}

	@Override
	public Object get(int i) {
		if ( i >= row.length ) {
			throw new IllegalArgumentException(
					"Given index [" + i + "] was outside the range of result tuple size [" + row.length + "] "
			);
		}
		return row[i];
	}

	@Override
	public Object[] toArray() {
		return row;
	}

	@Override
	public List<TupleElement<?>> getElements() {
		return tupleMetadata.getList();
	}

	@Override
	public String toString() {
		return Arrays.toString( row );
	}
}
