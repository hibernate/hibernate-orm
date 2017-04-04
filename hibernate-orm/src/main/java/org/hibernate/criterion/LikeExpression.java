/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import java.util.Locale;
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
		final String matchValue = ignoreCase ? value.toString().toLowerCase(Locale.ROOT) : value.toString();

		return new TypedValue[] { criteriaQuery.getTypedValue( criteria, propertyName, matchValue ) };
	}
}
