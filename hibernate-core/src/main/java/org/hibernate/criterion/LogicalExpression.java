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
