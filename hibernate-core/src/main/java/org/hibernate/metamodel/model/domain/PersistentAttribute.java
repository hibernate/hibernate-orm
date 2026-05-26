/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
	@Nonnull
	ManagedDomainType<D> getDeclaringType();

	@Nonnull
	JavaType<J> getAttributeJavaType();

	/**
	 * The classification of the attribute (is it a basic type, entity, etc)
	 */
	@Nonnull
	AttributeClassification getAttributeClassification();

	@Nonnull
	DomainType<?> getValueGraphType();

	@Nullable
	SimpleDomainType<?> getKeyGraphType();
}
