/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import static org.hibernate.internal.util.type.PrimitiveWrapperHelper.getDescriptorByPrimitiveType;

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
	@SuppressWarnings("unchecked")
	public <X> X get(TupleElement<X> tupleElement) {
		final Integer index = tupleMetadata.get( tupleElement );
		if ( index == null ) {
			throw new IllegalArgumentException(
					"Requested tuple element did not correspond to element in the result tuple"
			);
		}
		// index should be "in range" by nature of size check in ctor
		return (X) row[index];
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X get(String alias, Class<X> type) {
		final Object untyped = get( alias );
		if ( untyped != null ) {
			if ( !elementTypeMatches( type, untyped ) ) {
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
		return (X) untyped;
	}

	@Override
	public Object get(String alias) {
		Integer index = tupleMetadata.get( alias );
		if ( index == null ) {
			throw new IllegalArgumentException(
					"Given alias [" + alias + "] did not correspond to an element in the result tuple"
			);
		}
		// index should be "in range" by nature of size check in ctor
		return row[index];
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X get(int i, Class<X> type) {
		final Object result = get( i );
		if ( result != null && !elementTypeMatches( type, result ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Requested tuple value [index=%s, realType=%s] cannot be assigned to requested type [%s]",
							i,
							result.getClass().getName(),
							type.getName()
					)
			);
		}
		return (X) result;
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

	private <X> boolean elementTypeMatches(Class<X> type, Object untyped) {
		return type.isInstance( untyped )
			|| type.isPrimitive() && getDescriptorByPrimitiveType( type ).getWrapperClass().isInstance( untyped );
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
