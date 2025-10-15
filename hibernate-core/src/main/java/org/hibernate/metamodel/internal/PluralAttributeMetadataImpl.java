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

import static org.hibernate.metamodel.CollectionClassification.BAG;
import static org.hibernate.metamodel.CollectionClassification.ID_BAG;
import static org.hibernate.metamodel.CollectionClassification.LIST;
import static org.hibernate.metamodel.CollectionClassification.MAP;
import static org.hibernate.metamodel.CollectionClassification.ORDERED_MAP;
import static org.hibernate.metamodel.CollectionClassification.ORDERED_SET;
import static org.hibernate.metamodel.CollectionClassification.SET;
import static org.hibernate.metamodel.CollectionClassification.SORTED_MAP;
import static org.hibernate.metamodel.CollectionClassification.SORTED_SET;
import static org.hibernate.metamodel.internal.AttributeFactory.getSignatureType;

/**
 * @author Steve Ebersole
 */
class PluralAttributeMetadataImpl<X, Y, E>
		extends BaseAttributeMetadata<X, Y>
		implements PluralAttributeMetadata<X, Y, E> {
	private final CollectionClassification collectionClassification;
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

		collectionClassification = determineCollectionType( getJavaType(), propertyMapping );

		final var signatureType = getSignatureType( member );
		keyJavaType = getKeyJavaType( signatureType );
		elementJavaType = getElementJavaType( signatureType );

		elementValueContext = new ValueContext() {
			@Override
			public Value getHibernateValue() {
				return ( (Collection) propertyMapping.getValue() ).getElement();
			}

			@Override
			public Class<?> getJpaBindableType() {
				return elementJavaType;
			}

			@Override
			public ValueClassification getValueClassification() {
				return toValueClassification( elementClassification );
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
					return ( (Map) propertyMapping.getValue() ).getIndex();
				}

				@Override
				public Class<?> getJpaBindableType() {
					return keyJavaType;
				}

				@Override
				public ValueClassification getValueClassification() {
					return toValueClassification( listIndexOrMapKeyClassification );
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

	private Class<?> getElementJavaType(ParameterizedType signatureType) {
		return switch ( collectionClassification ) {
			case MAP, SORTED_MAP, ORDERED_MAP ->
					signatureType == null
							? Object.class
							: getClassFromGenericArgument( signatureType.getActualTypeArguments()[1] );
			case SET, SORTED_SET, ORDERED_SET, ARRAY, LIST, BAG, ID_BAG ->
					signatureType == null
							? Object.class
							: getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] );
		};
	}

	private Class<?> getKeyJavaType(ParameterizedType signatureType) {
		return switch ( collectionClassification ) {
			case ARRAY, LIST -> Integer.class;
			case MAP, SORTED_MAP, ORDERED_MAP ->
					signatureType == null
							? Object.class
							: getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] );
			default -> null;
		};
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
			final var upperBound = typeVariable.getBounds()[0];
			return getClassFromGenericArgument( upperBound );
		}
		else if ( type instanceof ParameterizedType parameterizedType ) {
			final var rawType = parameterizedType.getRawType();
			return getClassFromGenericArgument( rawType );
		}
		else if ( type instanceof WildcardType wildcardType ) {
			final var upperBound = wildcardType.getUpperBounds()[0];
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
			return LIST;
		}
		else if ( java.util.Set.class.isAssignableFrom( javaType ) ) {
			if ( collection.isSorted() ) {
				return SORTED_SET;
			}
			else if ( collection.hasOrder() ) {
				return ORDERED_SET;
			}
			else {
				return SET;
			}
		}
		else if ( java.util.Map.class.isAssignableFrom( javaType ) ) {
			if ( collection.isSorted() ) {
				return SORTED_MAP;
			}
			else if ( collection.hasOrder() ) {
				return ORDERED_MAP;
			}
			else {
				return MAP;
			}
		}
		else if ( java.util.Collection.class.isAssignableFrom( javaType ) ) {
			return collection.isIdentified() ? ID_BAG : BAG;

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
