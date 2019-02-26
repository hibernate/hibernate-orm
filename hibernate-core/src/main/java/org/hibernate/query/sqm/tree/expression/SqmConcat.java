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
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmConcat implements SqmExpression {
	private final SqmExpression lhsOperand;
	private final SqmExpression rhsOperand;

	private final BasicValuedExpressableType resultType;

	public SqmConcat(SqmExpression lhsOperand, SqmExpression rhsOperand) {
		this( lhsOperand, rhsOperand, (BasicValuedExpressableType) lhsOperand.getExpressableType() );
	}

	public SqmConcat(SqmExpression lhsOperand, SqmExpression rhsOperand, BasicValuedExpressableType resultType) {
		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;
		this.resultType = resultType;
	}

	public SqmExpression getLeftHandOperand() {
		return lhsOperand;
	}

	public SqmExpression getRightHandOperand() {
		return rhsOperand;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return resultType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Supplier<? extends BasicValuedExpressableType> getInferableType() {
		return () -> {
			// check LHS
			{
				final Supplier<? extends BasicValuedExpressableType> inference =
						(Supplier<? extends BasicValuedExpressableType>) lhsOperand.getInferableType();
				if ( inference != null ) {
					final BasicValuedExpressableType inferableType = inference.get();
					if ( inferableType != null ) {
						return inferableType;
					}
				}
			}

			// check RHS
			{
				final Supplier<? extends BasicValuedExpressableType> inference =
						(Supplier<? extends BasicValuedExpressableType>) rhsOperand.getInferableType();
				if ( inference != null ) {
					final BasicValuedExpressableType inferableType = inference.get();
					if ( inferableType != null ) {
						return inferableType;
					}
				}
			}

			return resultType;
		};
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
