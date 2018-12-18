/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.UnaryArithmeticOperator;

/**
 * Models unary arithmetic operation (unary plus and unary minus).
 *
 * @author Steve Ebersole
 */
public class UnaryArithmetic<T> extends AbstractExpression<T> {

	private final UnaryArithmeticOperator operator;
	private final ExpressionImplementor<T> operand;

	public UnaryArithmetic(
			UnaryArithmeticOperator operator,
			ExpressionImplementor<T> operand,
			CriteriaNodeBuilder builder) {
		super( operand.getJavaTypeDescriptor(), builder );
		this.operator = operator;
		this.operand = operand;
	}

	public UnaryArithmeticOperator getOperator() {
		return operator;
	}

	public ExpressionImplementor<T> getOperand() {
		return operand;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitUnaryArithmetic( this );
	}
}
