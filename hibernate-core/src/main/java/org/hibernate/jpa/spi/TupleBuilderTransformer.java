/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.type.Type;

/**
 * ResultTransformer adapter for handling Tuple results from HQL/JPQL queries
 *
 * @author Steve Ebersole
 */
public class TupleBuilderTransformer extends BasicTransformerAdapter {
	private List<TupleElement<?>> tupleElements;
	private Map<String, HqlTupleElementImpl> tupleElementsByAlias;

	public TupleBuilderTransformer(org.hibernate.Query hqlQuery) {
		final Type[] resultTypes = hqlQuery.getReturnTypes();
		final int tupleSize = resultTypes.length;

		this.tupleElements = CollectionHelper.arrayList( tupleSize );

		final String[] aliases = hqlQuery.getReturnAliases();
		final boolean hasAliases = aliases != null && aliases.length > 0;
		this.tupleElementsByAlias = hasAliases
				? CollectionHelper.<String, HqlTupleElementImpl>mapOfSize( tupleSize )
				: Collections.<String, HqlTupleElementImpl>emptyMap();

		for ( int i = 0; i < tupleSize; i++ ) {
			final HqlTupleElementImpl tupleElement = new HqlTupleElementImpl(
					i,
					aliases == null ? null : aliases[i],
					resultTypes[i]
			);
			tupleElements.add( tupleElement );
			if ( hasAliases ) {
				final String alias = aliases[i];
				if ( alias != null ) {
					tupleElementsByAlias.put( alias, tupleElement );
				}
			}
		}
	}

	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		if ( tuple.length != tupleElements.size() ) {
			throw new IllegalArgumentException(
					"Size mismatch between tuple result [" + tuple.length + "] and expected tuple elements [" +
							tupleElements.size() + "]"
			);
		}
		return new HqlTupleImpl( tuple );
	}

	public static class HqlTupleElementImpl<X> implements TupleElement<X> {
		private final int position;
		private final String alias;
		private final Type hibernateType;

		public HqlTupleElementImpl(int position, String alias, Type hibernateType) {
			this.position = position;
			this.alias = alias;
			this.hibernateType = hibernateType;
		}

		@Override
		public Class getJavaType() {
			return hibernateType.getReturnedClass();
		}

		@Override
		public String getAlias() {
			return alias;
		}

		public int getPosition() {
			return position;
		}

		public Type getHibernateType() {
			return hibernateType;
		}
	}

	public class HqlTupleImpl implements Tuple {
		private Object[] tuple;

		public HqlTupleImpl(Object[] tuple) {
			this.tuple = tuple;
		}

		@Override
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
			HqlTupleElementImpl tupleElement = tupleElementsByAlias.get( alias );
			if ( tupleElement == null ) {
				throw new IllegalArgumentException( "Unknown alias [" + alias + "]" );
			}
			return tuple[tupleElement.getPosition()];
		}

		@Override
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
			if ( i < 0 ) {
				throw new IllegalArgumentException( "requested tuple index must be greater than zero" );
			}
			if ( i > tuple.length ) {
				throw new IllegalArgumentException( "requested tuple index exceeds actual tuple size" );
			}
			return tuple[i];
		}

		@Override
		public Object[] toArray() {
			// todo : make a copy?
			return tuple;
		}

		@Override
		public List<TupleElement<?>> getElements() {
			return tupleElements;
		}

		@Override
		public <X> X get(TupleElement<X> tupleElement) {
			if ( HqlTupleElementImpl.class.isInstance( tupleElement ) ) {
				return get( ( (HqlTupleElementImpl) tupleElement ).getPosition(), tupleElement.getJavaType() );
			}
			else {
				return get( tupleElement.getAlias(), tupleElement.getJavaType() );
			}
		}
	}
}
