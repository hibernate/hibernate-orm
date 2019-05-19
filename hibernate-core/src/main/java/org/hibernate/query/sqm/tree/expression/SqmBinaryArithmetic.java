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
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Arrays.asList;
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
	protected void internalApplyInferableType(ExpressableType<?> type) {
		rhsOperand.applyInferableType( type );
		lhsOperand.applyInferableType( type );

		//don't try to cast Durations to Timestamps in addition expressions
		if ( !TypeConfiguration.isDuration( getExpressableType() ) ) {
			super.internalApplyInferableType(type);
		}
	}

	@Override
	public SqmExpression<?> evaluateDurationAddition(
			boolean negate,
			SqmExpression<?> timestamp,
			QueryEngine queryEngine,
			NodeBuilder nodeBuilder) {
		SqmExpression leftOperand = getLeftHandOperand();
		SqmExpression rightOperand = getRightHandOperand();

		if ( timestamp!=null
				&& isDuration( leftOperand.getExpressableType() )
				&& isDuration( rightOperand.getExpressableType() ) ) {
			// addition or subtraction of Durations
			SqmExpression left = leftOperand.evaluateDurationAddition(
					negate,
					timestamp,
					queryEngine,
					nodeBuilder
			);
			return rightOperand.evaluateDurationAddition(
					(operator == BinaryArithmeticOperator.SUBTRACT) != negate,
					left,
					queryEngine,
					nodeBuilder
			);
		}
		else if ( isTemporalType( leftOperand.getExpressableType() )
				&& isDuration( rightOperand.getExpressableType() ) ) {
			// must be addition/subtraction of a Duration to/from a timestamp
			if ( timestamp != null ) {
				throw new SemanticException("illegal operation");
			}
			return rightOperand.evaluateDurationAddition(
					operator==BinaryArithmeticOperator.SUBTRACT,
					leftOperand,
					queryEngine,
					nodeBuilder
			);
		}
		else {
			return this;
		}
	}

	@Override
	public SqmExpression<?> evaluateDuration(
			QueryEngine queryEngine,
			SqmExtractUnit<?> unit,
			BasicValuedExpressableType<Long> resultType,
			NodeBuilder nodeBuilder) {
		SqmExpression leftOperand = getLeftHandOperand();
		SqmExpression rightOperand = getRightHandOperand();

		if ( isTemporalType( leftOperand.getExpressableType() )
				&& isTemporalType( rightOperand.getExpressableType() ) ) {
			if ( getOperator() == BinaryArithmeticOperator.SUBTRACT ) {
				// the only kind of algebra we know how to do
				// on dates/timestamps is subtract them in the
				// presence of an 'of unit' operator, producing
				// a number
				return queryEngine.getSqmFunctionRegistry().findFunctionTemplate("timestampdiff").makeSqmFunctionExpression(
						asList(
								unit,
								//intentionally switch order!
								rightOperand,
								leftOperand
						),
						resultType,
						queryEngine
				);
			}
			else {
				// addition, multiplication, division of dates or
				// timestamps is always wrong and meaningless
				throw new SemanticException("illegal operator for temporal type " + getOperator());
			}
		}
		else {
			// we know how to do algebra on numbers, so
			// distribute the 'of unit' operator over
			// the terms, leaving us with a binary
			// operator expression applied to numbers
			return new SqmBinaryArithmetic<>(
					getOperator(),
					leftOperand.evaluateDuration( queryEngine, unit, resultType, nodeBuilder),
					rightOperand.evaluateDuration( queryEngine, unit, resultType, nodeBuilder),
					resultType,
					nodeBuilder
			);
		}

	}

	@Override
	public String asLoggableText() {
		return getOperator().toLoggableText( lhsOperand.asLoggableText(), rhsOperand.asLoggableText() );
	}

}
