/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.TypedValue;

/**
 * A criterion representing a "like" expression
 *
 * @author Scott Marlow
 * @author Steve Ebersole
 */
public class LikeExpression implements Criterion {
	private final String propertyName;
	private final Object value;
	private final Character escapeChar;
	private final boolean ignoreCase;

	protected LikeExpression(
			String propertyName,
			String value,
			Character escapeChar,
			boolean ignoreCase) {
		this.propertyName = propertyName;
		this.value = value;
		this.escapeChar = escapeChar;
		this.ignoreCase = ignoreCase;
	}

	protected LikeExpression(String propertyName, String value) {
		this( propertyName, value, null, false );
	}

	@SuppressWarnings("UnusedDeclaration")
	protected LikeExpression(String propertyName, String value, MatchMode matchMode) {
		this( propertyName, matchMode.toMatchString( value ) );
	}

	protected LikeExpression(
			String propertyName,
			String value,
			MatchMode matchMode,
			Character escapeChar,
			boolean ignoreCase) {
		this( propertyName, matchMode.toMatchString( value ), escapeChar, ignoreCase );
	}

	@Override
	public String toSqlString(Criteria criteria,CriteriaQuery criteriaQuery) {
		final Dialect dialect = criteriaQuery.getFactory().getDialect();
		final String[] columns = criteriaQuery.findColumns( propertyName, criteria );
		if ( columns.length != 1 ) {
			throw new HibernateException( "Like may only be used with single-column properties" );
		}

		final String escape = escapeChar == null ? "" : " escape \'" + escapeChar + "\'";
		final String column = columns[0];
		if ( ignoreCase ) {
			if ( dialect.supportsCaseInsensitiveLike() ) {
				return column +" " + dialect.getCaseInsensitiveLike() + " ?" + escape;
			}
			else {
				return dialect.getLowercaseFunction() + '(' + column + ')' + " like ?" + escape;
			}
		}
		else {
			return column + " like ?" + escape;
		}
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		final String matchValue = ignoreCase ? value.toString().toLowerCase() : value.toString();

		return new TypedValue[] { criteriaQuery.getTypedValue( criteria, propertyName, matchValue ) };
	}
}
