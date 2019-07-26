/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
			public Value getHibernateValue() {
				return getPropertyMapping().getValue();
			}

			public Class getJpaBindableType() {
				return getAttributeMetadata().getJavaType();
			}

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

			public AttributeMetadata getAttributeMetadata() {
				return SingularAttributeMetadataImpl.this;
			}
		};
	}

	public ValueContext getValueContext() {
		return valueContext;
	}
}
