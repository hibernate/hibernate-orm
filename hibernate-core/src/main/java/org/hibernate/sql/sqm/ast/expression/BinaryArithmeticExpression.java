/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class BinaryArithmeticExpression extends SelfReadingExpressionSupport implements Expression {
	private final Operation operation;
	private final Expression lhsOperand;
	private final Expression rhsOperand;
	private final BasicType resultType;

	public BinaryArithmeticExpression(
			Operation operation,
			Expression lhsOperand,
			Expression rhsOperand,
			BasicType resultType) {
		this.operation = operation;
		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;
		this.resultType = resultType;
	}

	@Override
	public BasicType getType() {
		return resultType;
	}

	@Override
	public void accept(SqlTreeWalker sqlTreeWalker) {
		sqlTreeWalker.visitBinaryArithmeticExpression( this );
	}

	public enum Operation {
		ADD {
			@Override
			public String getOperatorSqlText() {
				return "+";
			}
		},
		SUBTRACT {
			@Override
			public String getOperatorSqlText() {
				return "-";
			}
		},
		MULTIPLY {
			@Override
			public String getOperatorSqlText() {
				return "*";
			}
		},
		DIVIDE {
			@Override
			public String getOperatorSqlText() {
				return "/";
			}
		},
		QUOT {
			@Override
			public String getOperatorSqlText() {
				return "/";
			}
		};

		public abstract String getOperatorSqlText();
	}

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
	public Operation getOperation() {
		return operation;
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
