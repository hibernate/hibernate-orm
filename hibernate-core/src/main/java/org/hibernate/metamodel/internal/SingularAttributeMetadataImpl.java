/*
 * SPDX-License-Identifier: Apache-2.0
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
			AttributeClassification attributeClassification) {
		super( propertyMapping, ownerType, member, attributeClassification );
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
				return switch ( attributeClassification ) {
					case EMBEDDED -> ValueClassification.EMBEDDABLE;
					case BASIC -> ValueClassification.BASIC;
					default -> ValueClassification.ENTITY;
				};
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
