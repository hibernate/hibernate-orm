/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;


import static org.hibernate.query.sqm.BinaryArithmeticOperator.ADD;
import static org.hibernate.query.sqm.BinaryArithmeticOperator.SUBTRACT;
import static org.hibernate.type.spi.TypeConfiguration.isDuration;

/**
 * @author Steve Ebersole
 */
public class SqmBinaryArithmetic<T> extends AbstractSqmExpression<T> implements SqmSelectableNode<T> {
	private final SqmExpression<?> lhsOperand;
	private final BinaryArithmeticOperator operator;
	private final SqmExpression<?> rhsOperand;

	public SqmBinaryArithmetic(
			BinaryArithmeticOperator operator,
			SqmExpression<?> lhsOperand,
			SqmExpression<?> rhsOperand,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				(SqmBindableType<T>) // TODO: this cast is unsound
						nodeBuilder.getTypeConfiguration().resolveArithmeticType(
								lhsOperand.getExpressible(),
								rhsOperand.getExpressible(),
								operator
						),
				nodeBuilder
		);

		this.lhsOperand = lhsOperand;
		this.operator = operator;
		this.rhsOperand = rhsOperand;

		final SqmBindableType<?> lhsExpressible = lhsOperand.getExpressible();
		final SqmBindableType<?> rhsExpressible = rhsOperand.getExpressible();
		if ( lhsExpressible == null
				&& isDuration( rhsExpressible )
				&& ( operator == ADD || operator == SUBTRACT ) ) {
			return;
		}
		this.lhsOperand.applyInferableType( rhsExpressible );
		this.rhsOperand.applyInferableType( lhsExpressible );
	}

	public SqmBinaryArithmetic(
			BinaryArithmeticOperator operator,
			SqmExpression<?> lhsOperand,
			SqmExpression<?> rhsOperand,
			SqmBindableType<T> expressibleType,
			NodeBuilder nodeBuilder) {
		super( expressibleType, nodeBuilder );

		this.operator = operator;

		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;

		applyInferableType( expressibleType );
	}

	@Override
	public SqmBinaryArithmetic<T> copy(SqmCopyContext context) {
		final SqmBinaryArithmetic<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmBinaryArithmetic<T> expression = context.registerCopy(
				this,
				new SqmBinaryArithmetic<>(
						operator,
						lhsOperand.copy( context ),
						rhsOperand.copy( context ),
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitBinaryArithmeticExpression( this );
	}

	/**
	 * Get the left-hand operand.
	 *
	 * @return The left-hand operand.
	 */
	public SqmExpression<?> getLeftHandOperand() {
		return lhsOperand;
	}

	/**
	 * Get the operator
	 *
	 * @return The operator
	 */
	public BinaryArithmeticOperator getOperator() {
		return operator;
	}

	/**
	 * Get the right-hand operand.
	 *
	 * @return The right-hand operand.
	 */
	public SqmExpression<?> getRightHandOperand() {
		return rhsOperand;
	}

	@Override
	protected void internalApplyInferableType(SqmBindableType<?> type) {
		rhsOperand.applyInferableType( type );
		lhsOperand.applyInferableType( type );

		super.internalApplyInferableType( type );
	}

	@Override
	public String asLoggableText() {
		return getOperator().toLoggableText( lhsOperand.asLoggableText(), rhsOperand.asLoggableText() );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		lhsOperand.appendHqlString( hql, context );
		hql.append( ' ' );
		hql.append( operator.getOperatorSqlText() );
		hql.append( ' ' );
		rhsOperand.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmBinaryArithmetic<?> that
			&& this.operator == that.operator
			&& this.lhsOperand.equals( that.lhsOperand )
			&& this.rhsOperand.equals( that.rhsOperand );
	}

	@Override
	public int hashCode() {
		int result = lhsOperand.hashCode();
		result = 31 * result + operator.hashCode();
		result = 31 * result + rhsOperand.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmBinaryArithmetic<?> that
			&& this.operator == that.operator
			&& this.lhsOperand.isCompatible( that.lhsOperand )
			&& this.rhsOperand.isCompatible( that.rhsOperand );
	}

	@Override
	public int cacheHashCode() {
		int result = lhsOperand.cacheHashCode();
		result = 31 * result + operator.hashCode();
		result = 31 * result + rhsOperand.cacheHashCode();
		return result;
	}
}
