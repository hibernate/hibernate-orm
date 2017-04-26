/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class CountStarFunctionSqmExpression extends AbstractAggregateFunctionSqmExpression {
	public CountStarFunctionSqmExpression(boolean distinct, BasicValuedExpressableType resultType) {
		super( STAR, distinct, resultType );
	}

	@Override
	public String getFunctionName() {
		return CountFunctionSqmExpression.NAME;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitCountStarFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "COUNT(*)";
	}

	private static SqmExpression STAR = new SqmExpression() {
		@Override
		public BasicValuedExpressableType getExpressionType() {
			throw new UnsupportedOperationException( "Illegal attempt to visit * as argument of count(*)" );
		}

		@Override
		public BasicValuedExpressableType getInferableType() {
			throw new UnsupportedOperationException( "Illegal attempt to visit * as argument of count(*)" );
		}

		@Override
		public <T> T accept(SemanticQueryWalker<T> walker) {
			throw new UnsupportedOperationException( "Illegal attempt to visit * as argument of count(*)" );
		}

		@Override
		public String asLoggableText() {
			return "*";
		}
	};
}
