/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Order;

import org.hibernate.query.SortDirection;

/**
 * @author Steve Ebersole
 */
public interface JpaOrder extends Order, JpaCriteriaNode {

	/**
	 * The direction, ascending or descending, in which to sort
	 */
	SortDirection getSortDirection();

	/**
	 * Set the precedence of nulls for this order element
	 */
	JpaOrder nullPrecedence(Nulls precedence);

	/**
	 * The precedence for nulls for this order element
	 */
	Nulls getNullPrecedence();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant returns

	/**
	 * Reverse the sorting direction
	 */
	@Override
	JpaOrder reverse();

	/**
	 * The expression to sort by
	 */
	@Override
	JpaExpression<?> getExpression();
}
