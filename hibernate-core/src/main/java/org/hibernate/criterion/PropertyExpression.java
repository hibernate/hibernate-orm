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
import org.hibernate.internal.util.StringHelper;

/**
 * superclass for comparisons between two properties (with SQL binary operators)
 *
 * @author Gavin King
 */
public class PropertyExpression implements Criterion {
	private static final TypedValue[] NO_TYPED_VALUES = new TypedValue[0];

	private final String propertyName;
	private final String otherPropertyName;
	private final String op;

	protected PropertyExpression(String propertyName, String otherPropertyName, String op) {
		this.propertyName = propertyName;
		this.otherPropertyName = otherPropertyName;
		this.op = op;
	}

	public String getOp() {
		return op;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String[] lhsColumns = criteriaQuery.findColumns( propertyName, criteria );
		final String[] rhsColumns = criteriaQuery.findColumns( otherPropertyName, criteria );

		final String[] comparisons = StringHelper.add( lhsColumns, getOp(), rhsColumns );
		if ( comparisons.length > 1 ) {
			return '(' + StringHelper.join( " and ", comparisons ) + ')';
		}
		else {
			return comparisons[0];
		}
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		return NO_TYPED_VALUES;
	}

	@Override
	public String toString() {
		return propertyName + getOp() + otherPropertyName;
	}

}
