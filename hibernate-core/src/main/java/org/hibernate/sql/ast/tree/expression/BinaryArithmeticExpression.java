/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class BinaryArithmeticExpression implements Expression, DomainResultProducer {

	private final Expression lhsOperand;
	private final BinaryArithmeticOperator operator;
	private final Expression rhsOperand;

	private final MappingModelExpressable resultType;

	public BinaryArithmeticExpression(
			Expression lhsOperand,
			BinaryArithmeticOperator operator,
			Expression rhsOperand,
			MappingModelExpressable resultType) {
		this.operator = operator;
		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;
		this.resultType = resultType;
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return resultType;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitBinaryArithmeticExpression( this );
	}

//	@Override
//	public DomainResult createDomainResult(
//			String resultVariable,
//			DomainResultCreationState creationState) {
//		final SqlSelection sqlSelection = creationState.getSqlExpressionResolver().resolveSqlSelection(
//				this,
//				getType().getJavaTypeDescriptor(),
//				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
//		);
//		//noinspection unchecked
//		return new ScalarDomainResultImpl(
//				sqlSelection.getValuesArrayPosition(),
//				resultVariable,
//				resultType.getJavaTypeDescriptor()
//		);
//	}

	/**
	 * Get the left-hand operand.
	 *
	 * @return The left-hand operand.
	 */
	public Expression getLeftHandOperand() {
		return lhsOperand;
	}

	/**
	 * Get the operation
	 *
	 * @return The operation
	 */
	public BinaryArithmeticOperator getOperator() {
		return operator;
	}

	/**
	 * Get the right-hand operand.
	 *
	 * @return The right-hand operand.
	 */
	public Expression getRightHandOperand() {
		return rhsOperand;
	}
}
