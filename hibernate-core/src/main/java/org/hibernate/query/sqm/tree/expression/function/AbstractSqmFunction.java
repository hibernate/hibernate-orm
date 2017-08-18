/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFunction implements SqmFunction {
	private final AllowableFunctionReturnType resultType;

	public AbstractSqmFunction(AllowableFunctionReturnType resultType) {
		this.resultType = resultType;
	}

	@Override
	public AllowableFunctionReturnType getExpressionType() {
		return resultType;
	}

	@Override
	public AllowableFunctionReturnType getInferableType() {
		return getExpressionType();
	}

	@Override
	public QueryResult createQueryResult(
			Expression expression, String resultVariable, QueryResultCreationContext creationContext) {
		return new ScalarQueryResultImpl(
				resultVariable,
				creationContext.getSqlSelectionResolver().resolveSqlSelection( expression ),
				(BasicValuedExpressableType) getExpressionType()
		);
	}
}
