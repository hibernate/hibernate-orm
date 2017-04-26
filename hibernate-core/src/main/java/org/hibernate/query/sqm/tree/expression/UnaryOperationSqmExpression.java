/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public class UnaryOperationSqmExpression implements ImpliedTypeSqmExpression {
	public enum Operation {
		PLUS,
		MINUS
	}

	private final Operation operation;
	private final SqmExpression operand;

	private BasicValuedExpressableType typeDescriptor;

	public UnaryOperationSqmExpression(Operation operation, SqmExpression operand) {
		this( operation, operand, (BasicValuedExpressableType) operand.getExpressionType() );
	}

	public UnaryOperationSqmExpression(Operation operation, SqmExpression operand, BasicValuedExpressableType typeDescriptor) {
		this.operation = operation;
		this.operand = operand;
		this.typeDescriptor = typeDescriptor;
	}

	@Override
	public BasicValuedExpressableType getExpressionType() {
		return typeDescriptor;
	}

	@Override
	public BasicValuedExpressableType getInferableType() {
		return (BasicValuedExpressableType) operand.getExpressionType();
	}

	@Override
	public void impliedType(ExpressableType type) {
		if ( type != null ) {
			this.typeDescriptor = (BasicValuedExpressableType) type;
			if ( operand instanceof ImpliedTypeSqmExpression ) {
				( (ImpliedTypeSqmExpression) operand ).impliedType( type );
			}
		}
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitUnaryOperationExpression( this );
	}

	@Override
	public String asLoggableText() {
		return ( operation == Operation.MINUS ? '-' : '+' ) + operand.asLoggableText();
	}

	public SqmExpression getOperand() {
		return operand;
	}

	public Operation getOperation() {
		return operation;
	}
}
