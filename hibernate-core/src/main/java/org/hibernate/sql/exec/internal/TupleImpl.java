/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import java.util.List;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

/**
 * Implementation of the JPA Tuple contract
 *
 * @author Steve Ebersole
 */
public class TupleImpl implements Tuple {
	private final List<TupleElement<?>> tupleElements;
	private final Object[] row;

	public TupleImpl(List<TupleElement<?>> tupleElements, Object[] row) {
		this.tupleElements = tupleElements;
		this.row = row;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X get(TupleElement<X> tupleElement) {
		int index = tupleElements.indexOf( tupleElement );
		if ( index < 0 ) {
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
			if ( !type.isInstance( untyped ) ) {
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
		int index = -1;
		if ( alias != null ) {
			alias = alias.trim();
			if ( alias.length() > 0 ) {
				int i = 0;
				for ( TupleElement selection : tupleElements ) {
					if ( alias.equals( selection.getAlias() ) ) {
						index = i;
						break;
					}
					i++;
				}
			}
		}
		if ( index < 0 ) {
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
		if ( result != null && !type.isInstance( result ) ) {
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

	@Override
	public Object[] toArray() {
		return row;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<TupleElement<?>> getElements() {
		return tupleElements;
	}
}
