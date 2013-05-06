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
