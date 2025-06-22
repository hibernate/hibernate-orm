/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper;

/**
 * Contract for {@link PropertyMapper} implementations to expose whether they contain any property
 * that uses {@link org.hibernate.envers.internal.entities.PropertyData#isUsingModifiedFlag()}.
 *
 * @author Chris Cranford
 */
public interface ModifiedFlagMapperSupport {
	/**
	 * Returns whether the associated {@link PropertyMapper} has any properties that use
	 * the {@code witModifiedFlag} feature.
	 *
	 * @return {@code true} if a property uses {@code withModifiedFlag}, otherwise {@code false}.
	 */
	boolean hasPropertiesWithModifiedFlag();
}
