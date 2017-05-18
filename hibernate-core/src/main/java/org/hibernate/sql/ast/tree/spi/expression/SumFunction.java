/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.metamodel.queryable.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class SumFunction extends AbstractAggregateFunction implements AggregateFunction {
	public SumFunction(Expression argument, boolean distinct, BasicValuedExpressableType resultType) {
		super( argument, distinct, resultType );
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitSumFunction( this );
	}
}
