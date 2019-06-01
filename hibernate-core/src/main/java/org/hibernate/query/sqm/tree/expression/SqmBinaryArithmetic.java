/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.function.SqmByUnit;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.BinaryArithmeticOperator.ADD;
import static org.hibernate.query.BinaryArithmeticOperator.MULTIPLY;
import static org.hibernate.query.BinaryArithmeticOperator.SUBTRACT;
import static org.hibernate.type.spi.TypeConfiguration.isDuration;
import static org.hibernate.type.spi.TypeConfiguration.isTemporalType;

/**
 * @author Steve Ebersole
 */
public class SqmBinaryArithmetic<T> extends AbstractSqmExpression<T> {
	private final SqmExpression<?> lhsOperand;
	private final BinaryArithmeticOperator operator;
	private final SqmExpression<?> rhsOperand;

	public SqmBinaryArithmetic(
			BinaryArithmeticOperator operator,
			SqmExpression<?> lhsOperand,
			SqmExpression<?> rhsOperand,
			TypeConfiguration typeConfiguration,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				(ExpressableType<T>) typeConfiguration.resolveArithmeticType(
						(BasicValuedExpressableType) lhsOperand.getExpressableType(),
						(BasicValuedExpressableType) rhsOperand.getExpressableType(),
						operator
				),
				nodeBuilder
		);

		this.lhsOperand = lhsOperand;
		this.operator = operator;
		this.rhsOperand = rhsOperand;

		this.lhsOperand.applyInferableType( rhsOperand.getExpressableType() );
		this.rhsOperand.applyInferableType( lhsOperand.getExpressableType() );
	}

	public SqmBinaryArithmetic(
			BinaryArithmeticOperator operator,
			SqmExpression<?> lhsOperand,
			SqmExpression<?> rhsOperand,
			BasicValuedExpressableType<T> expressableType,
			NodeBuilder nodeBuilder) {
		super( expressableType, nodeBuilder );

		this.operator = operator;

		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;

		applyInferableType( expressableType );
	}

	@Override
	public BasicValuedExpressableType<T> getExpressableType() {
		return (BasicValuedExpressableType<T>) super.getExpressableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitBinaryArithmeticExpression( this );
	}

	/**
	 * Get the left-hand operand.
	 *
	 * @return The left-hand operand.
	 */
	public SqmExpression getLeftHandOperand() {
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
	public SqmExpression getRightHandOperand() {
		return rhsOperand;
	}

	@Override
	protected void internalApplyInferableType(ExpressableType<?> newType) {
		//don't cast Durations to Timestamps in addition expressions
		//don't cast scalars to Durations in multiplication expressions
		if ( !isDuration( getExpressableType() )
				&& !isDuration( newType ) ) {
			rhsOperand.applyInferableType( newType );
			lhsOperand.applyInferableType( newType );
		}

		super.internalApplyInferableType( newType );
	}

	@Override
	public String asLoggableText() {
		return getOperator().toLoggableText( lhsOperand.asLoggableText(), rhsOperand.asLoggableText() );
	}

}
