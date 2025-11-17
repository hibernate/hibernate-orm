/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.Nulls;
import org.hibernate.Incubating;
import org.hibernate.query.SortDirection;

/**
 * Represents the search order for a recursive CTE (common table expression).
 *
 * @see JpaCteCriteria
 */
@Incubating
public interface JpaSearchOrder extends JpaCriteriaNode {
	SortDirection getSortOrder();

	/**
	 * Set the precedence of nulls for this search order element
	 */
	JpaSearchOrder nullPrecedence(Nulls precedence);

	/**
	 * The precedence for nulls for this search order element
	 */
	Nulls getNullPrecedence();

	/**
	 * Whether ascending ordering is in effect.
	 * @return boolean indicating whether ordering is ascending
	 */
	boolean isAscending();

	/**
	 * Switch the ordering.
	 * @return a new <code>Order</code> instance with the reversed ordering
	 */
	JpaSearchOrder reverse();

	/**
	 * Return the CTE attribute that is used for ordering.
	 * @return CTE attribute used for ordering
	 */
	JpaCteCriteriaAttribute getAttribute();
}
