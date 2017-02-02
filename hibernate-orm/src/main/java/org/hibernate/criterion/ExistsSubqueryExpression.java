/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;

/**
 * Expression that checks the existence of rows in a sub-query
 *
 * @author Gavin King
 */
public class ExistsSubqueryExpression extends SubqueryExpression {
	/**
	 * Constructs the ExistsSubqueryExpression
	 *
	 * @param quantifier The "exists"/"not exists" sub-query quantifier
	 * @param dc The DetachedCriteria representing the sub-query
	 *
	 * @see Subqueries#exists
	 * @see Subqueries#notExists
	 */
	protected ExistsSubqueryExpression(String quantifier, DetachedCriteria dc) {
		super( null, quantifier, dc );
	}

	@Override
	protected String toLeftSqlString(Criteria criteria, CriteriaQuery outerQuery) {
		return "";
	}
}
