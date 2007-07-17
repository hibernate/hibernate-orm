package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.TypedValue;

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

	protected LikeExpression(
			String propertyName,
			String value) {
		this( propertyName, value, null, false );
	}

	protected LikeExpression(
			String propertyName,
			String value,
			MatchMode matchMode) {
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

	public String toSqlString(
			Criteria criteria,
			CriteriaQuery criteriaQuery) throws HibernateException {
		Dialect dialect = criteriaQuery.getFactory().getDialect();
		String[] columns = criteriaQuery.getColumnsUsingProjection( criteria, propertyName );
		if ( columns.length != 1 ) {
			throw new HibernateException( "Like may only be used with single-column properties" );
		}
		String lhs = ignoreCase
				? dialect.getLowercaseFunction() + '(' + columns[0] + ')'
	            : columns[0];
		return lhs + " like ?" + ( escapeChar == null ? "" : " escape \'" + escapeChar + "\'" );

	}

	public TypedValue[] getTypedValues(
			Criteria criteria,
			CriteriaQuery criteriaQuery) throws HibernateException {
		return new TypedValue[] {
				criteriaQuery.getTypedValue( criteria, propertyName, value.toString().toLowerCase() )
		};
	}
}
