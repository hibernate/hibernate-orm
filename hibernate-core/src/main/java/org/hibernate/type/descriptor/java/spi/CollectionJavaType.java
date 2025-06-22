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

	public CollectionJavaType(Class<? extends C> type, CollectionSemantics<C,?> semantics) {
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

	@Override
	public JavaType<C> createJavaType(
			ParameterizedType parameterizedType,
			TypeConfiguration typeConfiguration) {
		final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
		final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		switch ( semantics.getCollectionClassification() ) {
			case ARRAY:
				//noinspection unchecked
				return (JavaType<C>) new ArrayJavaType<>(
						javaTypeRegistry.resolveDescriptor(
								( (Class<?>) parameterizedType.getRawType() ).getComponentType()
						)
				);
			case BAG:
			case ID_BAG:
			case LIST:
			case SET:
			case SORTED_SET:
			case ORDERED_SET:
				//noinspection unchecked,rawtypes
				return new BasicCollectionJavaType(
						parameterizedType,
						javaTypeRegistry.resolveDescriptor( actualTypeArguments[actualTypeArguments.length - 1] ),
						semantics
				);

		}
		// Construct a basic java type that knows its parametrization
		//noinspection unchecked,rawtypes
		return new UnknownBasicJavaType(
				parameterizedType,
				new MapMutabilityPlan<>(
						(MapSemantics<Map<Object, Object>, Object, Object>) semantics,
						javaTypeRegistry.resolveDescriptor( actualTypeArguments[0] ),
						javaTypeRegistry.resolveDescriptor( actualTypeArguments[actualTypeArguments.length - 1] )
				)
		);
	}

	@Override
	public C fromString(CharSequence string) {
		throw new UnsupportedOperationException();
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

		if ( one instanceof PersistentCollection<?> pc ) {
			return pc.wasInitialized() && ( pc.isWrapper( another ) || pc.isDirectlyProvidedCollection( another ) );
		}

		if ( another instanceof PersistentCollection<?> pc ) {
			return pc.wasInitialized() && ( pc.isWrapper( one ) || pc.isDirectlyProvidedCollection( one ) );
		}

		return Objects.equals( one, another );
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
			final C copy = semantics.instantiateRaw( value.size(), null );

			for ( Map.Entry<K, V> entry : value.entrySet() ) {
				copy.put( keyPlan.deepCopy( entry.getKey() ), valuePlan.deepCopy( entry.getValue() ) );
			}
			return copy;
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
