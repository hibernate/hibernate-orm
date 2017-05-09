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
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class ConcatExpression implements Expression, SqlSelectable, Selectable {
	private final Expression lhsOperand;
	private final Expression rhsOperand;
	private final BasicValuedExpressableType type;

	public ConcatExpression(Expression lhsOperand, Expression rhsOperand) {
		this( lhsOperand, rhsOperand, (BasicType) lhsOperand.getType() );
	}

	public ConcatExpression(Expression lhsOperand, Expression rhsOperand, BasicValuedExpressableType type) {
		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;
		this.type = type;
	}

	public Expression getLeftHandOperand() {
		return lhsOperand;
	}

	public Expression getRightHandOperand() {
		return rhsOperand;
	}

	@Override
	public BasicValuedExpressableType getType() {
		return type;
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
		walker.visitConcatExpression( this );
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

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( getType() );
	}
}
