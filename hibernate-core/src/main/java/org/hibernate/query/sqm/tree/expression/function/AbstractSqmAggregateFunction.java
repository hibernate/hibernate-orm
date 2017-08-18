/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmAggregateFunction
		extends AbstractSqmFunction
		implements SqmAggregateFunction {
	private final SqmExpression argument;

	private boolean distinct;

	protected AbstractSqmAggregateFunction(SqmExpression argument, AllowableFunctionReturnType resultType) {
		super( resultType );
		this.argument = argument;
	}

	@Override
	public void makeDistinct() {
		distinct = true;
	}

	@Override
	public BasicValuedExpressableType getExpressionType() {
		return (BasicValuedExpressableType) super.getExpressionType();
	}

	@Override
	public BasicValuedExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public SqmExpression getArgument() {
		return argument;
	}

	@Override
	public boolean isDistinct() {
		return distinct;
	}

	@Override
	public QueryResult createQueryResult(
			Expression expression, String resultVariable, QueryResultCreationContext creationContext) {
		return new ScalarQueryResultImpl(
				resultVariable,
				creationContext.getSqlSelectionResolver().resolveSqlSelection( expression ),
				getInferableType()
		);
	}
}
