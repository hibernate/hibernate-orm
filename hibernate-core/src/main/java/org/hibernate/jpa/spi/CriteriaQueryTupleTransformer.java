/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import java.util.List;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.query.criteria.internal.ValueHandlerFactory;
import org.hibernate.transform.BasicTransformerAdapter;

/**
 * ResultTransformer adapter for handling Tuple results from Criteria queries
 *
 * @author Steve Ebersole
 */
public class CriteriaQueryTupleTransformer extends BasicTransformerAdapter {
	private final List<ValueHandlerFactory.ValueHandler> valueHandlers;
	private final List tupleElements;

	public CriteriaQueryTupleTransformer(List<ValueHandlerFactory.ValueHandler> valueHandlers, List tupleElements) {
		// todo : should these 2 sizes match *always*?
		this.valueHandlers = valueHandlers;
		this.tupleElements = tupleElements;
	}

	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		final Object[] valueHandlerResult;
		if ( valueHandlers == null ) {
			valueHandlerResult = tuple;
		}
		else {
			valueHandlerResult = new Object[tuple.length];
			for ( int i = 0; i < tuple.length; i++ ) {
				ValueHandlerFactory.ValueHandler valueHandler = valueHandlers.get( i );
				valueHandlerResult[i] = valueHandler == null
						? tuple[i]
						: valueHandler.convert( tuple[i] );
			}
		}

		return tupleElements == null
				? valueHandlerResult.length == 1 ? valueHandlerResult[0] : valueHandlerResult
				: new TupleImpl( tuple );

	}

	private class TupleImpl implements Tuple {
		private final Object[] tuples;

		private TupleImpl(Object[] tuples) {
			if ( tuples.length != tupleElements.size() ) {
				throw new IllegalArgumentException(
						"Size mismatch between tuple result [" + tuples.length
								+ "] and expected tuple elements [" + tupleElements.size() + "]"
				);
			}
			this.tuples = tuples;
		}

		public <X> X get(TupleElement<X> tupleElement) {
			int index = tupleElements.indexOf( tupleElement );
			if ( index < 0 ) {
				throw new IllegalArgumentException(
						"Requested tuple element did not correspond to element in the result tuple"
				);
			}
			// index should be "in range" by nature of size check in ctor
			return (X) tuples[index];
		}

		public Object get(String alias) {
			int index = -1;
			if ( alias != null ) {
				alias = alias.trim();
				if ( alias.length() > 0 ) {
					int i = 0;
					for ( TupleElement selection : (List<TupleElement>) tupleElements ) {
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
			return tuples[index];
		}

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

		public Object get(int i) {
			if ( i >= tuples.length ) {
				throw new IllegalArgumentException(
						"Given index [" + i + "] was outside the range of result tuple size [" + tuples.length + "] "
				);
			}
			return tuples[i];
		}

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

		public Object[] toArray() {
			return tuples;
		}

		public List<TupleElement<?>> getElements() {
			return tupleElements;
		}
	}
}
