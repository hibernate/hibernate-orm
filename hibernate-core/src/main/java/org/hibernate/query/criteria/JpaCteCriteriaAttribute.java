/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

/**
 * Describes the attribute of a {@link JpaCteCriteriaType}.
 */
@Incubating
public interface JpaCteCriteriaAttribute extends JpaCriteriaNode {

	/**
	 * The declaring type.
	 */
	JpaCteCriteriaType<?> getDeclaringType();

	/**
	 * The name of the attribute.
	 */
	String getName();

	/**
	 * The java type of the attribute.
	 */
	Class<?> getJavaType();
}
