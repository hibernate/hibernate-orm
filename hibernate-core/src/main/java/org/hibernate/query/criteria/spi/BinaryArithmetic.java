/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.BinaryArithmeticOperator;

/**
 * Models standard arithmetc operations with two operands.
 *
 * @author Steve Ebersole
 */
public class BinaryArithmetic<N extends Number> extends AbstractExpression<N> {

	public static Class<? extends Number> determineResultType(
			Class<? extends Number> argument1Type,
			Class<? extends Number> argument2Type) {
		return determineResultType( argument1Type, argument2Type, false );
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends Number> determineResultType(
			Class<? extends Number> argument1Type,
			Class<? extends Number> argument2Type,
			boolean isQuotientOperation) {
		if ( isQuotientOperation ) {
			return Number.class;
		}

		return ImplicitNumericExpressionTypeDeterminer.determineResultType( argument1Type, argument2Type );
	}


	private final ExpressionImplementor<? extends N> lhs;
	private final BinaryArithmeticOperator operator;
	private final ExpressionImplementor<? extends N> rhs;


	/**
	 * Creates an arithmethic operation based on 2 expressions.
	 *
	 * @param lhs The left-hand operand.
	 * @param operator The operator (type of operation).
	 * @param rhs The right-hand operand
	 * @param criteriaBuilder The builder for query components.
	 */
	@SuppressWarnings("unchecked")
	public BinaryArithmetic(
			ExpressionImplementor<? extends N> lhs,
			BinaryArithmeticOperator operator,
			ExpressionImplementor<? extends N> rhs,
			Class<N> resultType,
			CriteriaNodeBuilder criteriaBuilder) {
		super( resultType, criteriaBuilder );
		this.operator = operator;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	/**
	 * Creates an arithmethic operation based on an expression and a literal.
	 *
	 * @param lhs The left-hand operand
	 * @param operator The operator (type of operation).
	 * @param rhs The right-hand operand (the literal)
	 * @param criteriaBuilder The builder for query components.
	 */
	@SuppressWarnings("unchecked")
	public BinaryArithmetic(
			ExpressionImplementor<? extends N> lhs,
			BinaryArithmeticOperator operator,
			N rhs,
			Class<N> javaType,
			CriteriaNodeBuilder criteriaBuilder) {
		super( javaType, criteriaBuilder );
		this.operator = operator;
		this.lhs = lhs;
		this.rhs = criteriaBuilder.literal( rhs );
	}

	/**
	 * Creates an arithmetic operation based on an expression and a literal.
	 *
	 * @param lhs The left-hand operand (the literal)
	 * @param operator The operator (type of operation).
	 * @param rhs The right-hand operand
	 * @param criteriaBuilder The builder for query components.
	 */
	@SuppressWarnings("unchecked")
	public BinaryArithmetic(
			N lhs,
			BinaryArithmeticOperator operator,
			ExpressionImplementor<? extends N> rhs,
			Class<N> javaType,
			CriteriaNodeBuilder criteriaBuilder) {
		super( javaType, criteriaBuilder );
		this.operator = operator;
		this.lhs = criteriaBuilder.literal( lhs );
		this.rhs = rhs;
	}

	public ExpressionImplementor<? extends N> getLeftHandOperand() {
		return lhs;
	}

	public BinaryArithmeticOperator getOperator() {
		return operator;
	}

	public ExpressionImplementor<? extends N> getRightHandOperand() {
		return rhs;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitBinaryArithmetic( this );
	}
}
