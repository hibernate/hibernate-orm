/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class ParameterizedEntityTypeSqmExpression implements SqmExpression {
	private final ParameterSqmExpression parameterExpression;

	public ParameterizedEntityTypeSqmExpression(ParameterSqmExpression parameterExpression) {
		this.parameterExpression = parameterExpression;
	}

	@Override
	public ExpressableType getExpressionType() {
		return parameterExpression.getExpressionType();
	}

	@Override
	public ExpressableType getInferableType() {
		return parameterExpression.getExpressionType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitParameterizedEntityTypeExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "TYPE(" + parameterExpression.asLoggableText() + ")";
	}

	@Override
	public QueryResult createQueryResult(
			Expression expression,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		throw new UnsupportedOperationException( "At the moment, selection of an entity's type as a QueryResult is not supported" );
		// todo (6.0) : but could be ^^ - consider adding support for this (returning Class)
	}
}
