//$Id: SimpleExpression.java 7641 2005-07-25 04:57:05Z oneovthafew $
package org.hibernate.criterion;


import java.sql.Types;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.type.Type;

/**
 * superclass for "simple" comparisons (with SQL binary operators)
 * @author Gavin King
 */
public class SimpleExpression implements Criterion {

	private final String propertyName;
	private final Object value;
	private boolean ignoreCase;
	private final String op;

	protected SimpleExpression(String propertyName, Object value, String op) {
		this.propertyName = propertyName;
		this.value = value;
		this.op = op;
	}

	protected SimpleExpression(String propertyName, Object value, String op, boolean ignoreCase) {
		this.propertyName = propertyName;
		this.value = value;
		this.ignoreCase = ignoreCase;
		this.op = op;
	}

	public SimpleExpression ignoreCase() {
		ignoreCase = true;
		return this;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {

		String[] columns = criteriaQuery.getColumnsUsingProjection(criteria, propertyName);
		Type type = criteriaQuery.getTypeUsingProjection(criteria, propertyName);
		StringBuffer fragment = new StringBuffer();
		if (columns.length>1) fragment.append('(');
		SessionFactoryImplementor factory = criteriaQuery.getFactory();
		int[] sqlTypes = type.sqlTypes( factory );
		for ( int i=0; i<columns.length; i++ ) {
			boolean lower = ignoreCase && 
					( sqlTypes[i]==Types.VARCHAR || sqlTypes[i]==Types.CHAR );
			if (lower) {
				fragment.append( factory.getDialect().getLowercaseFunction() )
					.append('(');
			}
			fragment.append( columns[i] );
			if (lower) fragment.append(')');
			fragment.append( getOp() ).append("?");
			if ( i<columns.length-1 ) fragment.append(" and ");
		}
		if (columns.length>1) fragment.append(')');
		return fragment.toString();

	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		Object icvalue = ignoreCase ? value.toString().toLowerCase() : value;
		return new TypedValue[] { criteriaQuery.getTypedValue(criteria, propertyName, icvalue) };
	}

	public String toString() {
		return propertyName + getOp() + value;
	}

	protected final String getOp() {
		return op;
	}

}
