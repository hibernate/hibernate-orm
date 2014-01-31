/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.internal.metamodel.builder;

import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.PluralAttribute;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.jpa.internal.metamodel.AbstractManagedType;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;

/**
* @author Steve Ebersole
*/
public class PluralAttributeMetadataImpl<X,Y,E>
		extends BaseAttributeMetadata<X,Y>
		implements PluralAttributeMetadata<X,Y,E> {
	private final PluralAttribute.CollectionType attributeCollectionType;
	private final Attribute.PersistentAttributeType elementPersistentAttributeType;
	private final Attribute.PersistentAttributeType keyPersistentAttributeType;
	private final Class elementJavaType;
	private final Class keyJavaType;
	private final AttributeTypeDescriptor elementAttributeTypeDescriptor;
	private final AttributeTypeDescriptor keyAttributeTypeDescriptor;

	PluralAttributeMetadataImpl(
			PluralAttributeBinding attributeBinding,
			AbstractManagedType<X> ownerType,
			Member member,
			Attribute.PersistentAttributeType persistentAttributeType,
			Attribute.PersistentAttributeType elementPersistentAttributeType,
			Attribute.PersistentAttributeType keyPersistentAttributeType) {
		super( attributeBinding, ownerType, member, persistentAttributeType );
		this.attributeCollectionType = AttributeBuilder.determineCollectionType( getJavaType() );
		this.elementPersistentAttributeType = elementPersistentAttributeType;
		this.keyPersistentAttributeType = keyPersistentAttributeType;

		ParameterizedType signatureType = AttributeBuilder.getSignatureType( member );
		if ( keyPersistentAttributeType == null ) {
			elementJavaType = signatureType != null ?
					getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] ) :
					Object.class; //FIXME and honor targetEntity?
			keyJavaType = null;
		}
		else {
			keyJavaType = signatureType != null ?
					getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] ) :
					Object.class; //FIXME and honor targetEntity?
			elementJavaType = signatureType != null ?
					getClassFromGenericArgument( signatureType.getActualTypeArguments()[1] ) :
					Object.class; //FIXME and honor targetEntity?
		}

		this.elementAttributeTypeDescriptor = new AttributeTypeDescriptor() {
			@Override
			public org.hibernate.type.Type getHibernateType() {
				return getAttributeBinding().getPluralAttributeElementBinding()
						.getHibernateTypeDescriptor()
						.getResolvedTypeMapping();
			}

			public Class getBindableType() {
				return elementJavaType;
			}

			public ValueClassification getValueClassification() {
				switch ( PluralAttributeMetadataImpl.this.elementPersistentAttributeType ) {
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
		if ( keyPersistentAttributeType != null ) {
			this.keyAttributeTypeDescriptor = new AttributeTypeDescriptor() {
				@Override
				public org.hibernate.type.Type getHibernateType() {
					return ( (IndexedPluralAttributeBinding) getAttributeBinding() ).getPluralAttributeIndexBinding()
							.getHibernateTypeDescriptor()
							.getResolvedTypeMapping();
				}

				public Class getBindableType() {
					return keyJavaType;
				}

				public ValueClassification getValueClassification() {
					switch ( PluralAttributeMetadataImpl.this.keyPersistentAttributeType ) {
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
			keyAttributeTypeDescriptor = null;
		}
	}

	private Class<?> getClassFromGenericArgument(java.lang.reflect.Type type) {
		if ( type instanceof Class ) {
			return (Class) type;
		}
		else if ( type instanceof TypeVariable ) {
			final java.lang.reflect.Type upperBound = ( ( TypeVariable ) type ).getBounds()[0];
			return getClassFromGenericArgument( upperBound );
		}
		else if ( type instanceof ParameterizedType ) {
			final java.lang.reflect.Type rawType = ( (ParameterizedType) type ).getRawType();
			return getClassFromGenericArgument( rawType );
		}
		else {
			throw new AssertionFailure(
					"Fail to process type argument in a generic declaration. Member : " + getMemberDescription()
							+ " Type: " + type.getClass()
			);
		}
	}

	@Override
	public PluralAttributeBinding getAttributeBinding() {
		return (PluralAttributeBinding) super.getAttributeBinding();
	}

	public AttributeTypeDescriptor getElementAttributeTypeDescriptor() {
		return elementAttributeTypeDescriptor;
	}

	public PluralAttribute.CollectionType getAttributeCollectionType() {
		return attributeCollectionType;
	}

	public AttributeTypeDescriptor getMapKeyAttributeTypeDescriptor() {
		return keyAttributeTypeDescriptor;
	}
}
