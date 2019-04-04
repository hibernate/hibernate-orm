/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public class SqmConcat extends AbstractSqmExpression {
	private final SqmExpression lhsOperand;
	private final SqmExpression rhsOperand;

	public SqmConcat(SqmExpression lhsOperand, SqmExpression rhsOperand) {
		super( null );

		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;

		applyInferableType( StandardSpiBasicTypes.STRING );
	}

	public SqmConcat(SqmExpression lhsOperand, SqmExpression rhsOperand, BasicValuedExpressableType<?> resultType) {
		super( resultType );

		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;

		this.lhsOperand.applyInferableType( resultType );
		this.rhsOperand.applyInferableType( resultType );
	}

	public SqmExpression getLeftHandOperand() {
		return lhsOperand;
	}

	public SqmExpression getRightHandOperand() {
		return rhsOperand;
	}

	@Override
	public BasicValuedExpressableType<?> getExpressableType() {
		return (BasicValuedExpressableType<?>) super.getExpressableType();
	}

	@Override
	protected void internalApplyInferableType(ExpressableType<?> newType) {
		lhsOperand.applyInferableType( newType );
		rhsOperand.applyInferableType( newType );

		super.internalApplyInferableType( newType );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitConcatExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<concat>";
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}
}
