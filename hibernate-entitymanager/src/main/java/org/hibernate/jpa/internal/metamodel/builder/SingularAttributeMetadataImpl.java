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
import javax.persistence.metamodel.Attribute;

import org.hibernate.jpa.internal.metamodel.AbstractManagedType;
import org.hibernate.metamodel.spi.binding.AttributeBinding;

/**
* @author Steve Ebersole
*/
public class SingularAttributeMetadataImpl<X,Y>
		extends BaseAttributeMetadata<X,Y>
		implements SingularAttributeMetadata<X,Y> {
	private final AttributeTypeDescriptor attributeTypeDescriptor;

	public SingularAttributeMetadataImpl(
			AttributeBinding attributeBinding,
			AbstractManagedType<X> ownerType,
			Member member,
			Attribute.PersistentAttributeType persistentAttributeType) {
		super( attributeBinding, ownerType, member, persistentAttributeType );
		attributeTypeDescriptor = new AttributeTypeDescriptor() {
			@Override
			public org.hibernate.type.Type getHibernateType() {
				return getAttributeMetadata().getAttributeBinding().getHibernateTypeDescriptor().getResolvedTypeMapping();
			}

			@Override
			public Class getBindableType() {
				return getAttributeMetadata().getJavaType();
			}

			@Override
			public ValueClassification getValueClassification() {
				switch ( getPersistentAttributeType() ) {
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

			@Override
			public AttributeMetadata getAttributeMetadata() {
				return SingularAttributeMetadataImpl.this;
			}
		};
	}

	public AttributeTypeDescriptor getAttributeTypeDescriptor() {
		return attributeTypeDescriptor;
	}
}
