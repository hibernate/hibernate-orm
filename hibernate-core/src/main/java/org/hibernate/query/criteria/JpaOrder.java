/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.Order;

import org.hibernate.NullPrecedence;
import org.hibernate.SortOrder;

/**
 * @author Steve Ebersole
 */
public interface JpaOrder extends Order, JpaCriteriaNode {
	SortOrder getSortOrder();

	/**
	 * Set the precedence for nulls for this order element
	 */
	JpaOrder nullPrecedence(NullPrecedence precedence);

	/**
	 * The precedence for nulls for this order element
	 */
	NullPrecedence getNullPrecedence();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Co-variants

	@Override
	JpaOrder reverse();

	@Override
	JpaExpression<?> getExpression();
}
