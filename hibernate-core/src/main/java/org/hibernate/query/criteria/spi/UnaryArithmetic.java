/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Models unary arithmetic operation (unary plus and unary minus).
 *
 * @author Steve Ebersole
 */
public class UnaryArithmetic<T> extends AbstractExpression<T> {

	public enum Operation {
		UNARY_PLUS,
		UNARY_MINUS
	}

	private final Operation operation;
	private final ExpressionImplementor<T> operand;

	public UnaryArithmetic(
			Operation operation,
			ExpressionImplementor<T> operand,
			CriteriaNodeBuilder builder) {
		super( operand.getJavaTypeDescriptor(), builder );
		this.operation = operation;
		this.operand = operand;
	}

	public Operation getOperation() {
		return operation;
	}

	public ExpressionImplementor<T> getOperand() {
		return operand;
	}

	@Override
	public <R> R accept(CriteriaVisitor visitor) {
		return visitor.visitUnaryArithmetic( this );
	}
}
