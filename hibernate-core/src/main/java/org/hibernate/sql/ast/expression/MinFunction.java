/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression;

import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class MinFunction extends AbstractAggregateFunction implements AggregateFunction {
	public MinFunction(Expression argument, boolean distinct, BasicType resultType) {
		super( argument, distinct, resultType );
	}

	@Override
	public void accept(SqlAstSelectInterpreter sqlTreeWalker) {
		sqlTreeWalker.visitMinFunction( this );
	}
}
