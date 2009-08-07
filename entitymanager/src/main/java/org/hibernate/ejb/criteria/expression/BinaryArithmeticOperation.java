/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.ejb.criteria.expression;

import javax.persistence.criteria.Expression;

import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * Models standard arithmetc operations with two operands.
 *
 * @author Steve Ebersole
 */
public class BinaryArithmeticOperation<N extends Number>
		extends ExpressionImpl<N>
		implements BinaryOperatorExpression<N> {

	public static enum Operation {
		ADD, SUBTRACT, MULTIPLY, DIVIDE, QUOT, MOD
	}

	private final Operation operator;
	private final Expression<? extends N> rhs;
	private final Expression<? extends N> lhs;

	public BinaryArithmeticOperation(
			QueryBuilderImpl queryBuilder,
			Class<N> javaType,
			Operation operator,
			Expression<? extends N> rhs,
			Expression<? extends N> lhs) {
		super( queryBuilder, javaType );
		this.operator = operator;
		this.rhs = rhs;
		this.lhs = lhs;
	}

	public BinaryArithmeticOperation(
			QueryBuilderImpl queryBuilder,
			Class<N> javaType,
			Operation operator,
			Expression<? extends N> rhs,
			N lhs) {
		super( queryBuilder, javaType );
		this.operator = operator;
		this.rhs = rhs;
		this.lhs = new LiteralExpression<N>( queryBuilder, lhs );
	}

	public BinaryArithmeticOperation(
			QueryBuilderImpl queryBuilder,
			Class<N> javaType,
			Operation operator,
			N rhs,
			Expression<? extends N> lhs) {
		super( queryBuilder, javaType );
		this.operator = operator;
		this.rhs = new LiteralExpression<N>( queryBuilder, rhs );
		this.lhs = lhs;
	}

	public Operation getOperator() {
		return operator;
	}

	public Expression<? extends N> getRightHandOperand() {
		return rhs;
	}

	public Expression<? extends N> getLeftHandOperand() {
		return lhs;
	}
}
