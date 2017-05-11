/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.consume.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.result.internal.BasicScalarSelectionImpl;
import org.hibernate.sql.ast.produce.result.spi.ColumnReferenceResolver;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class BinaryArithmeticExpression
		implements Expression, SqlSelectable, Selectable {
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
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
		walker.visitBinaryArithmeticExpression( this );
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public Selection createSelection(
			Expression selectedExpression,
			String resultVariable,
			ColumnReferenceResolver columnReferenceResolver) {
		return new BasicScalarSelectionImpl(
				selectedExpression,
				resultVariable,
				this
		);
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( getType() );
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
