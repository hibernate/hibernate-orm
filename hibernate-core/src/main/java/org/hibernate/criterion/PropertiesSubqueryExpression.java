/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
