/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

import org.hibernate.Incubating;

/**
 * A tuple of values.
 *
 * @since 6.5
 */
@Incubating
public interface JpaValues {

	/**
	 * Returns the expressions of this tuple.
	 */
	List<? extends JpaExpression<?>> getExpressions();
}
