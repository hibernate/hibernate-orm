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
