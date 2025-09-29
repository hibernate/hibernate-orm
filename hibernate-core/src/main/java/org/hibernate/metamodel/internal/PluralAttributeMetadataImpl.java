/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.hibernate.AssertionFailure;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.ValueClassification;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * @author Steve Ebersole
 */
class PluralAttributeMetadataImpl<X, Y, E>
		extends BaseAttributeMetadata<X, Y>
		implements PluralAttributeMetadata<X, Y, E> {
	private final CollectionClassification collectionClassification;
	private final AttributeClassification elementClassification;
	private final AttributeClassification listIndexOrMapKeyClassification;
	private final Class<?> elementJavaType;
	private final Class<?> keyJavaType;
	private final ValueContext elementValueContext;
	private final ValueContext keyValueContext;

	PluralAttributeMetadataImpl(
			Property propertyMapping,
			ManagedDomainType<X> ownerType,
			Member member,
			AttributeClassification attributeClassification,
			AttributeClassification elementClassification,
			AttributeClassification listIndexOrMapKeyClassification) {
		super( propertyMapping, ownerType, member, attributeClassification );
		this.collectionClassification = determineCollectionType( getJavaType(), propertyMapping );
		this.elementClassification = elementClassification;
		this.listIndexOrMapKeyClassification = listIndexOrMapKeyClassification;

		final var signatureType = AttributeFactory.getSignatureType( member );
		switch ( collectionClassification ) {
			case MAP:
			case SORTED_MAP:
			case ORDERED_MAP: {
				keyJavaType = signatureType != null
						? getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] )
						: Object.class;
				elementJavaType = signatureType != null
						? getClassFromGenericArgument( signatureType.getActualTypeArguments()[1] )
						: Object.class;
				break;
			}
			case ARRAY:
			case LIST: {
				keyJavaType = Integer.class;
				elementJavaType = signatureType != null
						? getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] )
						: Object.class;
				break;
			}
			default: {
				elementJavaType = signatureType != null
						? getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] )
						: Object.class;
				keyJavaType = null;
			}
		}

		elementValueContext = new ValueContext() {
			@Override
			public Value getHibernateValue() {
				return ( (Collection) getPropertyMapping().getValue() ).getElement();
			}

			@Override
			public Class<?> getJpaBindableType() {
				return elementJavaType;
			}

			@Override
			public ValueClassification getValueClassification() {
				return toValueClassification(PluralAttributeMetadataImpl.this.elementClassification);
			}

			@Override
			public AttributeMetadata<X,Y> getAttributeMetadata() {
				return PluralAttributeMetadataImpl.this;
			}
		};

		// interpret the key, if one
		if ( listIndexOrMapKeyClassification != null ) {
			keyValueContext = new ValueContext() {
				@Override
				public Value getHibernateValue() {
					return ( (Map) getPropertyMapping().getValue() ).getIndex();
				}

				@Override
				public Class<?> getJpaBindableType() {
					return keyJavaType;
				}

				@Override
				public ValueClassification getValueClassification() {
					return toValueClassification(PluralAttributeMetadataImpl.this.listIndexOrMapKeyClassification);
				}

				@Override
				public AttributeMetadata<X,Y> getAttributeMetadata() {
					return PluralAttributeMetadataImpl.this;
				}
			};
		}
		else {
			keyValueContext = null;
		}
	}

	private static ValueClassification toValueClassification(AttributeClassification classification) {
		return switch ( classification ) {
			case EMBEDDED -> ValueClassification.EMBEDDABLE;
			case BASIC -> ValueClassification.BASIC;
			default -> ValueClassification.ENTITY;
		};
	}

	private Class<?> getClassFromGenericArgument(java.lang.reflect.Type type) {
		if ( type instanceof Class<?> clazz ) {
			return clazz;
		}
		else if ( type instanceof TypeVariable<?> typeVariable ) {
			final java.lang.reflect.Type upperBound = typeVariable.getBounds()[0];
			return getClassFromGenericArgument( upperBound );
		}
		else if ( type instanceof ParameterizedType parameterizedType ) {
			final java.lang.reflect.Type rawType = parameterizedType.getRawType();
			return getClassFromGenericArgument( rawType );
		}
		else if ( type instanceof WildcardType wildcardType ) {
			final java.lang.reflect.Type upperBound = wildcardType.getUpperBounds()[0];
			return getClassFromGenericArgument( upperBound );
		}
		else {
			throw new AssertionFailure(
					"Fail to process type argument in a generic declaration. Member : " + getMemberDescription()
							+ " Type: " + type.getClass()
			);
		}
	}

	public static CollectionClassification determineCollectionType(Class<?> javaType, Property property) {
		final var collection = (Collection) property.getValue();

		if ( java.util.List.class.isAssignableFrom( javaType ) ) {
			return CollectionClassification.LIST;
		}
		else if ( java.util.Set.class.isAssignableFrom( javaType ) ) {
			if ( collection.isSorted() ) {
				return CollectionClassification.SORTED_SET;
			}

			if ( collection.hasOrder() ) {
				return CollectionClassification.ORDERED_SET;
			}

			return CollectionClassification.SET;
		}
		else if ( java.util.Map.class.isAssignableFrom( javaType ) ) {
			if ( collection.isSorted() ) {
				return CollectionClassification.SORTED_MAP;
			}

			if ( collection.hasOrder() ) {
				return CollectionClassification.ORDERED_MAP;
			}

			return CollectionClassification.MAP;
		}
		else if ( java.util.Collection.class.isAssignableFrom( javaType ) ) {
			if ( collection.isIdentified() ) {
				return CollectionClassification.ID_BAG;
			}

			return CollectionClassification.BAG;
		}
		else if ( javaType.isArray() ) {
			return CollectionClassification.ARRAY;
		}
		else {
			throw new IllegalArgumentException( "Expecting collection type [" + javaType.getName() + "]" );
		}
	}

	@Override
	public ValueContext getElementValueContext() {
		return elementValueContext;
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return collectionClassification;
	}

	@Override
	public ValueContext getMapKeyValueContext() {
		return keyValueContext;
	}
}
