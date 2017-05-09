/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;
import org.hibernate.sql.ast.produce.result.internal.ReturnScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.ReturnResolutionContext;
import org.hibernate.sql.ast.consume.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;

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
	private final BasicValuedExpressableType type;

	public UnaryOperationExpression(Operation operation, Expression operand, BasicValuedExpressableType type) {
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
	public BasicValuedExpressableType getType() {
		return type;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( getType() );
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
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
