/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression;


import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class CountStarFunction extends AbstractAggregateFunction {
	public CountStarFunction(boolean distinct, BasicType resultType) {
		super( STAR, distinct, resultType );
	}

	private static Expression STAR = new StarExpression();

	@Override
	public void accept(SqlAstSelectInterpreter walker) {
		walker.visitCountStarFunction( this );
	}

	static class StarExpression implements Expression {
		@Override
		public Type getType() {
			return null;
		}

		@Override
		public void accept(SqlAstSelectInterpreter walker) {
		}

		@Override
		public Selectable getSelectable() {
			throw new UnsupportedOperationException(  );
		}
	}
}
