/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import jakarta.persistence.metamodel.Attribute;

import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Hibernate extension to the JPA {@link Attribute} contract
 *
 * @author Steve Ebersole
 */
public interface PersistentAttribute<D,J> extends Attribute<D,J> {
	@Override
	ManagedDomainType<D> getDeclaringType();

	JavaType<J> getAttributeJavaType();

	/**
	 * The classification of the attribute (is it a basic type, entity, etc)
	 */
	AttributeClassification getAttributeClassification();

	DomainType<?> getValueGraphType();
	SimpleDomainType<?> getKeyGraphType();
}
