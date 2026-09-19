/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.spi.VersionValue;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.type.descriptor.java.VersionJavaType;

/**
 * Describes the mapping of an entity's version
 *
 * @see jakarta.persistence.Version
 */
public interface EntityVersionMapping extends BasicValuedModelPart {

	String VERSION_ROLE_NAME = "{version}";

	static boolean matchesRoleName(@Nonnull String name) {
		return VERSION_ROLE_NAME.equals( name );
	}

	/**
	 * The attribute marked as the version
	 */
	@Nonnull
	@org.hibernate.Internal
	BasicAttributeMapping getVersionAttribute();

	/**
	 * The strategy for distinguishing between detached and transient
	 * state based on the version mapping.
	 *
	 * @see EntityIdentifierMapping#getUnsavedStrategy()
	 */
	@Nonnull
	VersionValue getUnsavedStrategy();

	@Nonnull
	@Override
	VersionJavaType<?> getJavaType();

	@Nonnull
	@Override
	default VersionJavaType<?> getExpressibleJavaType() {
		return (VersionJavaType<?>) getMappedType().getMappedJavaType();
	}

	@Nonnull
	@Override
	default AttributeMapping asAttributeMapping() {
		return getVersionAttribute();
	}
}
