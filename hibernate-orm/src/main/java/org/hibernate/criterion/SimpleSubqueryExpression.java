/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.TypedValue;

/**
 * A comparison between a constant value and the the result of a subquery
 *
 * @author Gavin King
 */
public class SimpleSubqueryExpression extends SubqueryExpression {
	private Object value;

	protected SimpleSubqueryExpression(Object value, String op, String quantifier, DetachedCriteria dc) {
		super( op, quantifier, dc );
		this.value = value;
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final TypedValue[] subQueryTypedValues = super.getTypedValues( criteria, criteriaQuery );
		final TypedValue[] result = new TypedValue[subQueryTypedValues.length+1];
		System.arraycopy( subQueryTypedValues, 0, result, 1, subQueryTypedValues.length );
		result[0] = new TypedValue( getTypes()[0], value );
		return result;
	}

	@Override
	protected String toLeftSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		return "?";
	}
}
