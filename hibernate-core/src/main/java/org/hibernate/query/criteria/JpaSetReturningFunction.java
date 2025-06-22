/*
 * SPDX-License-Identifier: Apache-2.0
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
