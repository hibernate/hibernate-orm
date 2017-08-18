/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class SqmAvgFunction
		extends AbstractSqmAggregateFunction
		implements SqmAggregateFunction {
	public static final String NAME = "avg";

	public SqmAvgFunction(SqmExpression argument) {
		super( argument, (AllowableFunctionReturnType) argument.getExpressionType() );
	}

	public SqmAvgFunction(SqmExpression argument, AllowableFunctionReturnType resultType) {
		super( argument, resultType );
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitAvgFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "AVG(" + getArgument().asLoggableText() + ")";
	}

}
