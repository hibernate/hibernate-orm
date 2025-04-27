/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

/**
 * @author Steve Ebersole
 */
public class SqmUnaryOperation<T> extends AbstractSqmExpression<T> implements SqmSelectableNode<T> {
	private final UnaryArithmeticOperator operation;
	private final SqmExpression<T> operand;

	public SqmUnaryOperation(UnaryArithmeticOperator operation, SqmExpression<T> operand) {
		this(
				operation,
				operand,
				operand.nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType(
						operand.getExpressible().getRelationalJavaType().getJavaType()
				)
		);
	}

	public SqmUnaryOperation(
			UnaryArithmeticOperator operation,
			SqmExpression<T> operand,
			SqmExpressible<T> inherentType) {
		super( inherentType, operand.nodeBuilder() );
		this.operation = operation;
		this.operand = operand;
	}

	@Override
	public SqmUnaryOperation<T> copy(SqmCopyContext context) {
		final SqmUnaryOperation<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmUnaryOperation<T> expression = context.registerCopy(
				this,
				new SqmUnaryOperation<>(
						operation,
						operand.copy( context ),
						getNodeType()
				)
		);
		copyTo( expression, context );
		return expression;
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
		return operation.getOperatorChar() + operand.asLoggableText();
	}
	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( operation.getOperatorChar() );
		operand.appendHqlString( hql, context );
	}
}
