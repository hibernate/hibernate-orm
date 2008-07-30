/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.TypedValue;
import org.hibernate.util.StringHelper;

/**
 * superclass for comparisons between two properties (with SQL binary operators)
 * @author Gavin King
 */
public class PropertyExpression implements Criterion {

	private final String propertyName;
	private final String otherPropertyName;
	private final String op;

	private static final TypedValue[] NO_TYPED_VALUES = new TypedValue[0];

	protected PropertyExpression(String propertyName, String otherPropertyName, String op) {
		this.propertyName = propertyName;
		this.otherPropertyName = otherPropertyName;
		this.op = op;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		String[] xcols = criteriaQuery.getColumnsUsingProjection(criteria, propertyName);
		String[] ycols = criteriaQuery.getColumnsUsingProjection(criteria, otherPropertyName);
		String result = StringHelper.join(
			" and ",
			StringHelper.add(xcols, getOp(), ycols)
		);
		if (xcols.length>1) result = '(' + result + ')';
		return result;
		//TODO: get SQL rendering out of this package!
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return NO_TYPED_VALUES;
	}

	public String toString() {
		return propertyName + getOp() + otherPropertyName;
	}

	public String getOp() {
		return op;
	}

}
