/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaExpression;

/**
 * Models standard arithmetc operations with two operands.
 *
 * @author Steve Ebersole
 */
public class BinaryArithmetic<N extends Number> extends AbstractExpression<N> {

	public enum Operation {
		ADD,
		SUBTRACT,
		MULTIPLY,
		DIVIDE,
		QUOT,
		MOD,
		;
	}

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
	private final Operation operator;
	private final ExpressionImplementor<? extends N> rhs;


	/**
	 * Creates an arithmethic operation based on 2 expressions.
	 *
	 * @param criteriaBuilder The builder for query components.
	 * @param resultType The operation result type
	 * @param operator The operator (type of operation).
	 * @param lhs The left-hand operand.
	 * @param rhs The right-hand operand
	 */
	public BinaryArithmetic(
			ExpressionImplementor<? extends N> lhs,
			Operation operator,
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
	 * @param criteriaBuilder The builder for query components.
	 * @param javaType The operation result type
	 * @param operator The operator (type of operation).
	 * @param lhs The left-hand operand
	 * @param rhs The right-hand operand (the literal)
	 */
	public BinaryArithmetic(
			ExpressionImplementor<? extends N> lhs,
			Operation operator,
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
	 * @param criteriaBuilder The builder for query components.
	 * @param javaType The operation result type
	 * @param operator The operator (type of operation).
	 * @param lhs The left-hand operand (the literal)
	 * @param rhs The right-hand operand
	 */
	public BinaryArithmetic(
			N lhs,
			Operation operator,
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

	public Operation getOperator() {
		return operator;
	}

	public ExpressionImplementor<? extends N> getRightHandOperand() {
		return rhs;
	}

	@Override
	public <R> R accept(CriteriaVisitor visitor) {
		return visitor.visitBinaryArithmetic( this );
	}
}
