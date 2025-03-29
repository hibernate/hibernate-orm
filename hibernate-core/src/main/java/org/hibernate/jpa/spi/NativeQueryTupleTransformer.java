/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import org.hibernate.HibernateException;
import org.hibernate.query.TypedTupleTransformer;
import org.hibernate.transform.ResultTransformer;

import static java.util.Locale.ROOT;

/**
 * A {@link ResultTransformer} for handling JPA {@link Tuple} results from native queries.
 *
 * @author Arnold Galovics
 */
public class NativeQueryTupleTransformer implements ResultTransformer<Tuple>, TypedTupleTransformer<Tuple> {

	public static final NativeQueryTupleTransformer INSTANCE = new NativeQueryTupleTransformer();

	@Override
	public Tuple transformTuple(Object[] tuple, String[] aliases) {
		return new NativeTupleImpl( tuple, aliases );
	}

	@Override
	public Class<Tuple> getTransformedType() {
		return Tuple.class;
	}

	private static class NativeTupleElementImpl<X> implements TupleElement<X> {

		private final Class<? extends X> javaType;

		private final String alias;

		public NativeTupleElementImpl(Class<? extends X> javaType, String alias) {
			this.javaType = javaType;
			this.alias = alias;
		}

		@Override
		public Class<? extends X> getJavaType() {
			return javaType;
		}

		@Override
		public String getAlias() {
			return alias;
		}
	}

	private static class NativeTupleImpl implements Tuple {

		private final Object[] tuple;

		private final int size;

		private final Map<String, Object> aliasToValue = new LinkedHashMap<>();
		private final Map<String, String> aliasReferences = new LinkedHashMap<>();

		public NativeTupleImpl(Object[] tuple, String[] aliases) {
			if ( tuple == null ) {
				throw new HibernateException( "Tuple must not be null" );
			}
			if ( aliases == null ) {
				throw new HibernateException( "Aliases must not be null" );
			}
			if ( tuple.length != aliases.length ) {
				throw new HibernateException( "Got different size of tuples and aliases" );
			}
			this.tuple = tuple;
			for ( int i = 0; i < tuple.length; i++ ) {
				final String alias = aliases[i];
				if ( alias != null ) {
					aliasToValue.put( alias, tuple[i] );
					aliasReferences.put( alias.toLowerCase(ROOT), alias );
				}
			}
			size = tuple.length;
		}

		@Override
		public <X> X get(String alias, Class<X> type) {
			final Object untyped = get( alias );

			return ( untyped != null ) ? type.cast( untyped ) : null;
		}

		@Override
		public Object get(String alias) {
			final String aliasReference = aliasReferences.get( alias.toLowerCase(ROOT) );
			if ( aliasReference != null && aliasToValue.containsKey( aliasReference ) ) {
				return aliasToValue.get( aliasReference );
			}
			throw new IllegalArgumentException( "Unknown alias [" + alias + "]" );
		}

		@Override
		public <X> X get(int i, Class<X> type) {
			final Object untyped = get( i );

			return ( untyped != null ) ? type.cast( untyped ) : null;
		}

		@Override
		public Object get(int i) {
			if ( i < 0 ) {
				throw new IllegalArgumentException( "requested tuple index must be greater than zero" );
			}
			if ( i >= size ) {
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
		public String toString() {
			return Arrays.toString( tuple );
		}

		@Override
		public List<TupleElement<?>> getElements() {
			List<TupleElement<?>> elements = new ArrayList<>( size );

			for ( Map.Entry<String, Object> entry : aliasToValue.entrySet() ) {
				elements.add( new NativeTupleElementImpl<>( getValueClass( entry.getValue() ), entry.getKey() ) );
			}
			return elements;
		}

		private Class<?> getValueClass(Object value) {
			Class<?> valueClass = Object.class;
			if ( value != null ) {
				valueClass = value.getClass();
			}
			return valueClass;
		}

		@Override
		public <X> X get(TupleElement<X> tupleElement) {
			return get( tupleElement.getAlias(), tupleElement.getJavaType() );
		}
	}
}
