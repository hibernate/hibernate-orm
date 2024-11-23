/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Member;

import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.ValueClassification;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeMetadataImpl<X, Y> extends BaseAttributeMetadata<X, Y>
		implements SingularAttributeMetadata<X, Y> {
	private final ValueContext valueContext;

	SingularAttributeMetadataImpl(
			Property propertyMapping,
			ManagedDomainType<X> ownerType,
			Member member,
			AttributeClassification attributeClassification,
			MetadataContext metadataContext) {
		super( propertyMapping, ownerType, member, attributeClassification, metadataContext );
		valueContext = new ValueContext() {
			@Override
			public Value getHibernateValue() {
				return getPropertyMapping().getValue();
			}

			@Override
			public Class<Y> getJpaBindableType() {
				return getAttributeMetadata().getJavaType();
			}

			@Override
			public ValueClassification getValueClassification() {
				switch ( attributeClassification ) {
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
			public AttributeMetadata<X,Y> getAttributeMetadata() {
				return SingularAttributeMetadataImpl.this;
			}
		};
	}

	@Override
	public ValueContext getValueContext() {
		return valueContext;
	}
}
