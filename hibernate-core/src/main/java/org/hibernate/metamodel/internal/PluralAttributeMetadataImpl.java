/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.hibernate.annotations.common.AssertionFailure;
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
	private final Class elementJavaType;
	private final Class keyJavaType;
	private final ValueContext elementValueContext;
	private final ValueContext keyValueContext;

	PluralAttributeMetadataImpl(
			Property propertyMapping,
			ManagedDomainType<X> ownerType,
			Member member,
			AttributeClassification attributeClassification,
			AttributeClassification elementClassification,
			AttributeClassification listIndexOrMapKeyClassification,
			MetadataContext metadataContext) {
		super( propertyMapping, ownerType, member, attributeClassification, metadataContext );
		this.collectionClassification = determineCollectionType( getJavaType(), propertyMapping );
		this.elementClassification = elementClassification;
		this.listIndexOrMapKeyClassification = listIndexOrMapKeyClassification;

		final ParameterizedType signatureType = AttributeFactory.getSignatureType( member );
		switch ( collectionClassification ) {
			case MAP:
			case SORTED_MAP:
			case ORDERED_MAP: {
				this.keyJavaType = signatureType != null
						? getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] )
						: Object.class;

				this.elementJavaType = signatureType != null
						? getClassFromGenericArgument( signatureType.getActualTypeArguments()[1] )
						: Object.class;

				break;
			}
			case ARRAY:
			case LIST: {
				this.keyJavaType = Integer.class;

				this.elementJavaType = signatureType != null
						? getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] )
						: Object.class;

				break;
			}
			default: {
				this.elementJavaType = signatureType != null
						? getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] )
						: Object.class;
				this.keyJavaType = null;
			}
		}

		this.elementValueContext = new ValueContext() {
			public Value getHibernateValue() {
				return ( (Collection) getPropertyMapping().getValue() ).getElement();
			}

			public Class getJpaBindableType() {
				return elementJavaType;
			}

			public ValueClassification getValueClassification() {
				switch ( PluralAttributeMetadataImpl.this.elementClassification ) {
					case EMBEDDED: {
						return ValueClassification.EMBEDDABLE;
					}
					case BASIC: {
						return ValueClassification.BASIC;
					}
					default: {
						return ValueClassification.ENTITY;
					}
				}
			}

			public AttributeMetadata getAttributeMetadata() {
				return PluralAttributeMetadataImpl.this;
			}
		};

		// interpret the key, if one
		if ( this.listIndexOrMapKeyClassification != null ) {
			this.keyValueContext = new ValueContext() {
				public Value getHibernateValue() {
					return ( (Map) getPropertyMapping().getValue() ).getIndex();
				}

				public Class getJpaBindableType() {
					return keyJavaType;
				}

				public ValueClassification getValueClassification() {
					switch ( PluralAttributeMetadataImpl.this.listIndexOrMapKeyClassification ) {
						case EMBEDDED: {
							return ValueClassification.EMBEDDABLE;
						}
						case BASIC: {
							return ValueClassification.BASIC;
						}
						default: {
							return ValueClassification.ENTITY;
						}
					}
				}

				public AttributeMetadata getAttributeMetadata() {
					return PluralAttributeMetadataImpl.this;
				}
			};
		}
		else {
			keyValueContext = null;
		}
	}

	private Class<?> getClassFromGenericArgument(java.lang.reflect.Type type) {
		if ( type instanceof Class ) {
			return (Class) type;
		}
		else if ( type instanceof TypeVariable ) {
			final java.lang.reflect.Type upperBound = ( (TypeVariable) type ).getBounds()[0];
			return getClassFromGenericArgument( upperBound );
		}
		else if ( type instanceof ParameterizedType ) {
			final java.lang.reflect.Type rawType = ( (ParameterizedType) type ).getRawType();
			return getClassFromGenericArgument( rawType );
		}
		else if ( type instanceof WildcardType ) {
			final java.lang.reflect.Type upperBound = ( (WildcardType) type ).getUpperBounds()[0];
			return getClassFromGenericArgument( upperBound );
		}
		else {
			throw new AssertionFailure(
					"Fail to process type argument in a generic declaration. Member : " + getMemberDescription()
							+ " Type: " + type.getClass()
			);
		}
	}

	public static CollectionClassification determineCollectionType(Class javaType, Property property) {
		final Collection collection = (Collection) property.getValue();

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
				return CollectionClassification.IDBAG;
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

	public ValueContext getElementValueContext() {
		return elementValueContext;
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return collectionClassification;
	}

	public ValueContext getMapKeyValueContext() {
		return keyValueContext;
	}
}
