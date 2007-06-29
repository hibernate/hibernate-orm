//$Id: PropertySubqueryExpression.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.criterion;

import org.hibernate.Criteria;

/**
 * A comparison between a property value in the outer query and the
 * result of a subquery
 * @author Gavin King
 */
public class PropertySubqueryExpression extends SubqueryExpression {
	private String propertyName;

	protected PropertySubqueryExpression(String propertyName, String op, String quantifier, DetachedCriteria dc) {
		super(op, quantifier, dc);
		this.propertyName = propertyName;
	}

	protected String toLeftSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		return criteriaQuery.getColumn(criteria, propertyName);
	}

}
