/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.produce.result.internal.ReturnScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.consume.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAggregateFunction implements AggregateFunction {
	private final Expression argument;
	private final boolean distinct;
	private final BasicValuedExpressableType resultType;

	public AbstractAggregateFunction(Expression argument, boolean distinct, BasicValuedExpressableType resultType) {
		this.argument = argument;
		this.distinct = distinct;
		this.resultType = resultType;
	}

	@Override
	public boolean isDistinct() {
		return distinct;
	}

	@Override
	public BasicValuedExpressableType getType() {
		return resultType;
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
	public Expression getArgument() {
		return argument;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( getType() );
	}

	@Override
	public Return toQueryReturn(QueryResultCreationContext returnResolutionContext, String resultVariable) {
		return new ReturnScalarImpl(
				this,
				returnResolutionContext.resolveSqlSelection( this ),
				resultVariable,
				getType()
		);
	}
}
