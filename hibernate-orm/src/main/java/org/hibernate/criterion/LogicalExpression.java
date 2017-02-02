/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.engine.spi.TypedValue;

/**
 * Superclass of binary logical expressions
 *
 * @author Gavin King
 */
public class LogicalExpression implements Criterion {
	private final Criterion lhs;
	private final Criterion rhs;
	private final String op;

	protected LogicalExpression(Criterion lhs, Criterion rhs, String op) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.op = op;
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) {
		final TypedValue[] lhsTypedValues = lhs.getTypedValues( criteria, criteriaQuery );
		final TypedValue[] rhsTypedValues = rhs.getTypedValues( criteria, criteriaQuery );

		final TypedValue[] result = new TypedValue[ lhsTypedValues.length + rhsTypedValues.length ];
		System.arraycopy( lhsTypedValues, 0, result, 0, lhsTypedValues.length );
		System.arraycopy( rhsTypedValues, 0, result, lhsTypedValues.length, rhsTypedValues.length );
		return result;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		return '('
				+ lhs.toSqlString( criteria, criteriaQuery )
				+ ' '
				+ getOp()
				+ ' '
				+ rhs.toSqlString( criteria, criteriaQuery )
				+ ')';
	}

	public String getOp() {
		return op;
	}

	@Override
	public String toString() {
		return lhs.toString() + ' ' + getOp() + ' ' + rhs.toString();
	}
}
