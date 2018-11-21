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
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmBinaryArithmetic implements SqmExpression {
	private final Operation operation;
	private final SqmExpression lhsOperand;
	private final SqmExpression rhsOperand;

	private BasicValuedExpressableType expressionType;

	public SqmBinaryArithmetic(
			Operation operation,
			SqmExpression lhsOperand,
			SqmExpression rhsOperand,
			BasicValuedExpressableType expressionType) {
		this.operation = operation;
		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;
		this.expressionType = expressionType;
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return expressionType.getJavaTypeDescriptor();
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
	 * Get the operation
	 *
	 * @return The operation
	 */
	public Operation getOperation() {
		return operation;
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
	public BasicValuedExpressableType getExpressableType() {
		return expressionType;
	}

	@Override
	public Supplier<? extends BasicValuedExpressableType> getInferableType() {
		return this::getExpressableType;
	}


	@Override
	public String asLoggableText() {
		return getOperation().apply( lhsOperand.asLoggableText(), rhsOperand.asLoggableText() );
	}



	public enum Operation {
		ADD {
			@Override
			String apply(String lhs, String rhs) {
				return applyPrimitive( lhs, '+', rhs );
			}
		},
		SUBTRACT {
			@Override
			String apply(String lhs, String rhs) {
				return applyPrimitive( lhs, '-', rhs );
			}
		},
		MULTIPLY {
			@Override
			String apply(String lhs, String rhs) {
				return applyPrimitive( lhs, '*', rhs );
			}
		},
		DIVIDE {
			@Override
			String apply(String lhs, String rhs) {
				return applyPrimitive( lhs, '/', rhs );
			}
		},
		QUOT {
			@Override
			String apply(String lhs, String rhs) {
				return applyPrimitive( lhs, '/', rhs );
			}
		},
		MODULO {
			@Override
			String apply(String lhs, String rhs) {
//				return lhs + " % " + rhs;
				return "mod(" + lhs + "," + rhs + ")";
			}
		};

		abstract String apply(String lhs, String rhs);

		private static String applyPrimitive(String lhs, char operator, String rhs) {
			return '(' + lhs + operator + rhs + ')';
		}
	}
}
