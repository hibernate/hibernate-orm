/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.function.Supplier;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public class SqmUnaryOperation extends AbstractInferableTypeSqmExpression {

	public enum Operation {
		PLUS,
		MINUS
	}

	private final Operation operation;
	private final SqmExpression operand;

	public SqmUnaryOperation(Operation operation, SqmExpression operand) {
		this( operation, operand, (BasicValuedExpressableType) operand.getExpressableType() );
	}

	public SqmUnaryOperation(Operation operation, SqmExpression operand, BasicValuedExpressableType inherentType) {
		super( inherentType );
		this.operation = operation;
		this.operand = operand;
	}

	public SqmExpression getOperand() {
		return operand;
	}

	public Operation getOperation() {
		return operation;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return (BasicValuedExpressableType) super.getExpressableType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Supplier<? extends BasicValuedExpressableType> getInferableType() {
		return (Supplier<? extends BasicValuedExpressableType>) super.getInferableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitUnaryOperationExpression( this );
	}

	@Override
	public String asLoggableText() {
		return ( operation == Operation.MINUS ? '-' : '+' ) + operand.asLoggableText();
	}
}
