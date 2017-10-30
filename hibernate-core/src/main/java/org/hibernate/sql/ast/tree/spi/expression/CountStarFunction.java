/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class CountStarFunction extends AbstractAggregateFunction {
	public CountStarFunction(boolean distinct, BasicValuedExpressableType resultType) {
		super( STAR, distinct, resultType );
	}

	private static Expression STAR = new StarExpression();

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitCountStarFunction( this );
	}

	@Override
	public SqlSelection createSqlSelection(int jdbcPosition) {
		return new SqlSelectionImpl(
				jdbcPosition,
				this,
				getType().getBasicType().getSqlSelectionReader()
		);
	}

	static class StarExpression implements Expression {
		@Override
		public BasicValuedExpressableType getType() {
			return null;
		}

		@Override
		public SqlSelection createSqlSelection(int jdbcPosition) {
			throw new UnsupportedOperationException( "The star (`*`) in a `count(*) expression cannot be selected" );
		}

		@Override
		public void accept(SqlAstWalker  walker) {
		}
	}
}
