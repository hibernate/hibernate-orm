/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.exec.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.exec.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.internal.BasicValuedNonNavigableSelection;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;

/**
 * @author Steve Ebersole
 */
public class BinaryArithmeticExpression
		implements Expression, SqlSelectable, Selectable {
	private final Operation operation;
	private final Expression lhsOperand;
	private final Expression rhsOperand;
	private final BasicValuedExpressableType resultType;

	public BinaryArithmeticExpression(
			Operation operation,
			Expression lhsOperand,
			Expression rhsOperand,
			BasicValuedExpressableType resultType) {
		this.operation = operation;
		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;
		this.resultType = resultType;
	}

	@Override
	public BasicValuedExpressableType getType() {
		return resultType;
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitBinaryArithmeticExpression( this );
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( getType() );
	}

	@Override
	public Selection createSelection(
			Expression selectedExpression,
			String resultVariable) {
		assert selectedExpression == this;

		return new BasicValuedNonNavigableSelection(
				selectedExpression,
				resultVariable,
				this
		);
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
