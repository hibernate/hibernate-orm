/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

/**
 * @author Steve Ebersole
 */
public class SqmUnaryOperation<T> extends AbstractSqmExpression<T> implements SqmSelectableNode<T> {
	private final UnaryArithmeticOperator operation;
	private final SqmExpression operand;

	public SqmUnaryOperation(
			UnaryArithmeticOperator operation,
			SqmExpression<T> operand) {
		this( operation, operand, operand.getNodeType() );
	}

	public SqmUnaryOperation(
			UnaryArithmeticOperator operation,
			SqmExpression<T> operand,
			SqmExpressable<T> inherentType) {
		super( inherentType, operand.nodeBuilder() );
		this.operation = operation;
		this.operand = operand;
	}

	public SqmExpression getOperand() {
		return operand;
	}

	public UnaryArithmeticOperator getOperation() {
		return operation;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitUnaryOperationExpression( this );
	}

	@Override
	public String asLoggableText() {
		return ( operation == UnaryArithmeticOperator.UNARY_MINUS ? '-' : '+' ) + operand.asLoggableText();
	}
}
