/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression;

import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.ast.select.SqlSelectable;
import org.hibernate.sql.convert.results.internal.ReturnScalarImpl;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sql.exec.results.process.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionReader;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class UnaryOperationExpression implements Expression, SqlSelectable, Selectable {
	public enum Operation {
		PLUS,
		MINUS
	}

	private final Operation operation;
	private final Expression operand;
	private final BasicType type;

	public UnaryOperationExpression(Operation operation, Expression operand, BasicType type) {
		this.operation = operation;
		this.operand = operand;
		this.type = type;
	}

	public Operation getOperation() {
		return operation;
	}

	public Expression getOperand() {
		return operand;
	}

	@Override
	public BasicType getType() {
		return type;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( getType() );
	}

	@Override
	public void accept(SqlAstSelectInterpreter walker) {
		walker.visitUnaryOperationExpression( this );
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public Expression getSelectedExpression() {
		return this;
	}

	@Override
	public Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable) {
		return new ReturnScalarImpl(
				this,
				returnResolutionContext.resolveSqlSelection( this ),
				resultVariable,
				getType()
		);
	}

}
