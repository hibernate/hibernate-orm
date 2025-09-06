/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import org.hibernate.SharedSessionContract;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.MapSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Extension of the general JavaType for "collection types"
 *
 * @apiNote "Collection types" are defined loosely here to cover mapping
 * collection types other than those from the "Java Collection Framework".
 *
 * @see CollectionSemantics
 *
 * @author Steve Ebersole
 */
public class CollectionJavaType<C> extends AbstractClassJavaType<C> {
	private final CollectionSemantics<C,?> semantics;

	public CollectionJavaType(Class<C> type, CollectionSemantics<C,?> semantics) {
		super( type );
		this.semantics = semantics;
	}

	public CollectionSemantics<C,?> getSemantics() {
		return semantics;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		// none
		return null;
	}

	@Override @SuppressWarnings({"unchecked", "rawtypes"})
	public JavaType<C> createJavaType(
			ParameterizedType parameterizedType,
			TypeConfiguration typeConfiguration) {
		final Type[] typeArguments = parameterizedType.getActualTypeArguments();
		final var registry = typeConfiguration.getJavaTypeRegistry();
		return switch ( semantics.getCollectionClassification() ) {
			case ARRAY -> {
				final var arrayClass = (Class<?>) parameterizedType.getRawType();
				yield (JavaType<C>) new ArrayJavaType<>( registry.resolveDescriptor( arrayClass.getComponentType() ) );
			}
			case BAG, ID_BAG, LIST, SET, SORTED_SET, ORDERED_SET ->
					new BasicCollectionJavaType(
							parameterizedType,
							registry.getDescriptor( typeArguments[typeArguments.length-1] ),
							semantics
					);
			case MAP, ORDERED_MAP, SORTED_MAP ->
				// Construct a basic java type that knows its parametrization
					new UnknownBasicJavaType(
							parameterizedType,
							new MapMutabilityPlan(
									(MapSemantics) semantics,
									registry.getDescriptor( typeArguments[0] ),
									registry.getDescriptor( typeArguments[typeArguments.length-1] )
							)
					);
		};
	}

	@Override
	public <X> X unwrap(C value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> C wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean areEqual(C one, C another) {
		if ( one == another ) {
			return true;
		}
		else if ( one instanceof PersistentCollection<?> collection ) {
			return wraps( collection, another );
		}
		else if ( another instanceof PersistentCollection<?> collection ) {
			return wraps( collection, one );
		}
		else {
			return Objects.equals( one, another );
		}
	}

	private static <C> boolean wraps(PersistentCollection<?> collection, C other) {
		return collection.wasInitialized()
			&& collection.isWrapper( other );
	}

	@Override
	public int extractHashCode(C x) {
		throw new UnsupportedOperationException();
	}

	private static class MapMutabilityPlan<C extends Map<K, V>, K, V> implements MutabilityPlan<C> {

		private final MapSemantics<C, K, V> semantics;
		private final MutabilityPlan<K> keyPlan;
		private final MutabilityPlan<V> valuePlan;

		public MapMutabilityPlan(
				MapSemantics<C, K, V> semantics,
				JavaType<K> keyType,
				JavaType<V> valueType) {
			this.semantics = semantics;
			this.keyPlan = keyType.getMutabilityPlan();
			this.valuePlan = valueType.getMutabilityPlan();
		}

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public C deepCopy(C value) {
			if ( value == null ) {
				return null;
			}
			else {
				final C copy = semantics.instantiateRaw( value.size(), null );
				for ( var entry : value.entrySet() ) {
					copy.put( keyPlan.deepCopy( entry.getKey() ),
							valuePlan.deepCopy( entry.getValue() ) );
				}
				return copy;
			}
		}

		@Override
		public Serializable disassemble(C value, SharedSessionContract session) {
			return (Serializable) deepCopy( value );
		}

		@Override
		public C assemble(Serializable cached, SharedSessionContract session) {
			//noinspection unchecked
			return deepCopy( (C) cached );
		}

	}
}
