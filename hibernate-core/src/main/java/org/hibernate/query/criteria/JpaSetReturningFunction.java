/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

/**
 * A set returning function criteria.
 */
@Incubating
public interface JpaSetReturningFunction<T> extends JpaCriteriaNode {

	/**
	 * The name of the function.
	 */
	String getFunctionName();

}
