/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

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
			MetamodelImplementor domainModel,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				(SqmExpressable<T>) domainModel.getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) lhsOperand.getNodeType(),
						(BasicValuedExpressableType) rhsOperand.getNodeType(),
						operator
				),
				nodeBuilder
		);

		this.lhsOperand = lhsOperand;
		this.operator = operator;
		this.rhsOperand = rhsOperand;

		this.lhsOperand.applyInferableType( rhsOperand.getNodeType() );
		this.rhsOperand.applyInferableType( lhsOperand.getNodeType() );
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
	public BasicValuedExpressableType<T> getNodeType() {
		return (BasicValuedExpressableType<T>) super.getNodeType();
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
	protected void internalApplyInferableType(SqmExpressable<?> type) {
		rhsOperand.applyInferableType( type );
		lhsOperand.applyInferableType( type );

		super.internalApplyInferableType( type );
	}

	@Override
	public String asLoggableText() {
		return getOperator().toLoggableText( lhsOperand.asLoggableText(), rhsOperand.asLoggableText() );
	}

}
