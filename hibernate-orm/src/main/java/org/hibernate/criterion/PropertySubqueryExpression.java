/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;

/**
 * A comparison between a property value in the outer query and the result of a subquery
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PropertySubqueryExpression extends SubqueryExpression {
	private String propertyName;

	protected PropertySubqueryExpression(String propertyName, String op, String quantifier, DetachedCriteria dc) {
		super( op, quantifier, dc );
		this.propertyName = propertyName;
	}

	@Override
	protected String toLeftSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		return criteriaQuery.getColumn( criteria, propertyName );
	}

}
