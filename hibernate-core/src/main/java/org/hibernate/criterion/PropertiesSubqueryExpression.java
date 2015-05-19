/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.internal.util.StringHelper;

/**
 * A comparison between several properties value in the outer query and the result of a multicolumn subquery.
 *
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
		final StringBuilder left = new StringBuilder( "(" );
		final String[] sqlColumnNames = new String[propertyNames.length];
		for ( int i = 0; i < sqlColumnNames.length; ++i ) {
			sqlColumnNames[i] = outerQuery.getColumn( criteria, propertyNames[i] );
		}
		left.append( StringHelper.join( ", ", sqlColumnNames ) );
		return left.append( ")" ).toString();
	}
}
