/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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

import org.hibernate.ejb.criteria.ParameterRegistry;
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

	/**
	 * Helper for determining the appropriate operation return type based on one of the operands as an expression.
	 *
	 * @param defaultType The default return type to use if we cannot determine the java type of 'expression' operand.
	 * @param expression The operand.
	 *
	 * @return The appropriate return type.
	 */
	public static Class<? extends Number> determineReturnType(
			Class<? extends Number> defaultType,
			Expression<? extends Number> expression) {
		return expression == null || expression.getJavaType() == null 
				? defaultType
				: expression.getJavaType();
	}

	/**
	 * Helper for determining the appropriate operation return type based on one of the operands as a literal.
	 *
	 * @param defaultType The default return type to use if we cannot determine the java type of 'numberLiteral' operand.
	 * @param numberLiteral The operand.
	 *
	 * @return The appropriate return type.
	 */
	public static Class<? extends Number> determineReturnType(
			Class<? extends Number> defaultType,
			Number numberLiteral) {
		return numberLiteral == null ? defaultType : numberLiteral.getClass();
	}

	/**
	 * Creates an arithmethic operation based on 2 expressions.
	 *
	 * @param queryBuilder The builder for query components.
	 * @param resultType The operation result type
	 * @param operator The operator (type of operation).
	 * @param rhs The right-hand operand
	 * @param lhs The left-hand operand.
	 */
	public BinaryArithmeticOperation(
			QueryBuilderImpl queryBuilder,
			Class<N> resultType,
			Operation operator,
			Expression<? extends N> rhs,
			Expression<? extends N> lhs) {
		super( queryBuilder, resultType );
		this.operator = operator;
		this.rhs = rhs;
		this.lhs = lhs;
	}

	/**
	 * Creates an arithmethic operation based on an expression and a literal.
	 *
	 * @param queryBuilder The builder for query components.
	 * @param resultType The operation result type
	 * @param operator The operator (type of operation).
	 * @param rhs The right-hand operand
	 * @param lhs The left-hand operand (the literal).
	 */
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

	/**
	 * Creates an arithmethic operation based on an expression and a literal.
	 *
	 * @param queryBuilder The builder for query components.
	 * @param resultType The operation result type
	 * @param operator The operator (type of operation).
	 * @param rhs The right-hand operand (the literal).
	 * @param lhs The left-hand operand
	 */
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

	/**
	 * {@inheritDoc}
	 */
	public Expression<? extends N> getRightHandOperand() {
		return rhs;
	}

	/**
	 * {@inheritDoc}
	 */
	public Expression<? extends N> getLeftHandOperand() {
		return lhs;
	}

	/**
	 * {@inheritDoc}
	 */
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getRightHandOperand(), registry );
		Helper.possibleParameter( getLeftHandOperand(), registry );
	}

}
