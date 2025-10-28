/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
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

	public SqmUnaryOperation(
			UnaryArithmeticOperator operation,
			SqmExpression<T> operand,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		this(
				operation,
				operand,
				(SqmBindableType<T>) // TODO: this cast is unsound
						nodeBuilder.getTypeConfiguration()
								.resolveArithmeticType( operand.getExpressible() ),
				nodeBuilder
		);
	}

	public SqmUnaryOperation(
			UnaryArithmeticOperator operation,
			SqmExpression<T> operand,
			SqmBindableType<T> inherentType,
			NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
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
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	public SqmExpression<T> getOperand() {
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

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmUnaryOperation<?> that
			&& operation == that.getOperation()
			&& operand.equals( that.getOperand() );
	}

	@Override
	public int hashCode() {
		int result = operation.hashCode();
		result = 31 * result + operand.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmUnaryOperation<?> that
				&& operation == that.getOperation()
				&& operand.isCompatible( that.getOperand() );
	}

	@Override
	public int cacheHashCode() {
		int result = operation.hashCode();
		result = 31 * result + operand.cacheHashCode();
		return result;
	}
}
