package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.internal.util.StringHelper;

/**
 * A comparison between several properties value in the outer query and the result of a multicolumn subquery.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class PropertiesSubqueryExpression extends SubqueryExpression {
	private final String[] propertyNames;

	protected PropertiesSubqueryExpression(String[] propertyNames, String op, DetachedCriteria dc) {
		super( op, null, dc );
		this.propertyNames = propertyNames;
	}

	@Override
	protected String toLeftSqlString(Criteria criteria, CriteriaQuery outerQuery) {
		StringBuilder left = new StringBuilder( "(" );
		final String[] sqlColumnNames = new String[propertyNames.length];
		for ( int i = 0; i < sqlColumnNames.length; ++i ) {
			sqlColumnNames[i] = outerQuery.getColumn( criteria, propertyNames[i] );
		}
		left.append( StringHelper.join( ", ", sqlColumnNames ) );
		return left.append( ")" ).toString();
	}
}
