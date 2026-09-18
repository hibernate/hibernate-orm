/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;

/**
 * Describes the attribute of a {@link JpaCteCriteriaType}.
 */
@Incubating(since = "6.3")
public interface JpaCteCriteriaAttribute extends JpaCriteriaNode {

	/**
	 * The declaring type.
	 */
	@Nonnull
	JpaCteCriteriaType<?> getDeclaringType();

	/**
	 * The name of the attribute.
	 */
	@Nonnull
	String getName();

	/**
	 * The java type of the attribute.
	 *
	 * @throws IllegalStateException if the Java type has not yet been determined
	 */
	@Nonnull
	Class<?> getJavaType();
}
