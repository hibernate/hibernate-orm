/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;
import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class CountStarFunction extends AbstractAggregateFunction {
	public CountStarFunction(boolean distinct, BasicType resultType) {
		super( STAR, distinct, resultType );
	}

	private static Expression STAR = new Expression() {
		@Override
		public Type getType() {
			return null;
		}

		@Override
		public ReturnReader getReturnReader(int startPosition, boolean shallow, SessionFactoryImplementor sessionFactory) {
			throw new UnsupportedOperationException(  );
		}

		@Override
		public void accept(SqlTreeWalker sqlTreeWalker) {
			throw new UnsupportedOperationException(  );
		}
	};

	@Override
	public void accept(SqlTreeWalker sqlTreeWalker) {
		sqlTreeWalker.visitCountStarFunction( this );
	}
}
