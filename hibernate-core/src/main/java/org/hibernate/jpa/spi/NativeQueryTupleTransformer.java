/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.hibernate.HibernateException;
import org.hibernate.query.TypedTupleTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Locale.ROOT;

/**
 * A {@link TypedTupleTransformer} for handling JPA {@link Tuple} results from native queries.
 *
 * @author Arnold Galovics
 */
public class NativeQueryTupleTransformer implements TypedTupleTransformer<Tuple> {

	@Nonnull
	public static final NativeQueryTupleTransformer INSTANCE = new NativeQueryTupleTransformer();

	@Override
	@Nonnull
	public Tuple transformTuple(@Nonnull Object[] tuple, @Nonnull String[] aliases) {
		return new NativeTupleImpl( tuple, aliases );
	}

	@Override
	@Nonnull
	public Class<Tuple> getTransformedType() {
		return Tuple.class;
	}

	private record NativeTupleElementImpl<X>
			(@Nonnull Class<? extends X> javaType, @Nullable String alias)
					implements TupleElement<X> {

		@Override
		public @Nonnull Class<? extends X> getJavaType() {
			return javaType;
		}

		@Override
		public @Nullable String getAlias() {
			return alias;
		}
	}

	private static class NativeTupleImpl implements Tuple {

		@Nonnull
		private final Object[] tuple;

		private final int size;

		@Nonnull
		private final Map<String, Object> aliasToValue = new LinkedHashMap<>();
		@Nonnull
		private final Map<String, String> aliasReferences = new LinkedHashMap<>();

		public NativeTupleImpl(@Nonnull Object[] tuple, @Nonnull String[] aliases) {
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
		@Nullable
		public <X> X get(@Nonnull String alias, @Nonnull Class<X> type) {
			final Object untyped = get( alias );
			return untyped == null ? null : type.cast( untyped );
		}

		@Override
		@Nullable
		public Object get(@Nonnull String alias) {
			final String aliasReference = aliasReferences.get( alias.toLowerCase(ROOT) );
			if ( aliasReference != null && aliasToValue.containsKey( aliasReference ) ) {
				return aliasToValue.get( aliasReference );
			}
			throw new IllegalArgumentException( "Unknown alias [" + alias + "]" );
		}

		@Override
		@Nullable
		public <X> X get(int i, @Nonnull Class<X> type) {
			final Object untyped = get( i );
			return untyped == null ? null : type.cast( untyped );
		}

		@Override
		@Nullable
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
		@Nonnull
		public Object[] toArray() {
			return tuple.clone();
		}

		@Override
		@Nonnull
		public String toString() {
			return Arrays.toString( tuple );
		}


		@Override
		public boolean equals(@Nullable Object obj) {
			return obj instanceof NativeTupleImpl that
				&& Objects.equals( this.aliasToValue, that.aliasToValue );
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode( tuple );
		}


		@Override
		@Nonnull
		public List<TupleElement<?>> getElements() {
			final List<TupleElement<?>> elements = new ArrayList<>( size );
			for ( var entry : aliasToValue.entrySet() ) {
				elements.add( new NativeTupleElementImpl<>( getValueClass( entry.getValue() ), entry.getKey() ) );
			}
			return elements;
		}

		@Nonnull
		private Class<?> getValueClass(@Nullable Object value) {
			return value == null ? Object.class : value.getClass();
		}

		@Override
		@Nullable
		public <X> X get(@Nonnull TupleElement<X> tupleElement) {
			final String alias = tupleElement.getAlias();
			if ( alias == null ) {
				throw new IllegalArgumentException( "TupleElement has no alias" );
			}
			return tupleElement.getJavaType().cast( get( alias ) );
		}
	}
}
