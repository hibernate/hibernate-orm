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
import org.hibernate.engine.spi.TypedValue;

/**
 * Superclass of binary logical expressions
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

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {

		TypedValue[] lhstv = lhs.getTypedValues(criteria, criteriaQuery);
		TypedValue[] rhstv = rhs.getTypedValues(criteria, criteriaQuery);
		TypedValue[] result = new TypedValue[ lhstv.length + rhstv.length ];
		System.arraycopy(lhstv, 0, result, 0, lhstv.length);
		System.arraycopy(rhstv, 0, result, lhstv.length, rhstv.length);
		return result;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {

		return '(' +
			lhs.toSqlString(criteria, criteriaQuery) +
			' ' +
			getOp() +
			' ' +
			rhs.toSqlString(criteria, criteriaQuery) +
			')';
	}

	public String getOp() {
		return op;
	}

	public String toString() {
		return lhs.toString() + ' ' + getOp() + ' ' + rhs.toString();
	}
}
