//$Id: ExistsSubqueryExpression.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.criterion;

import org.hibernate.Criteria;

/**
 * @author Gavin King
 */
public class ExistsSubqueryExpression extends SubqueryExpression {

	protected String toLeftSqlString(Criteria criteria, CriteriaQuery outerQuery) {
		return "";
	}
	
	protected ExistsSubqueryExpression(String quantifier, DetachedCriteria dc) {
		super(null, quantifier, dc);
	}
}
