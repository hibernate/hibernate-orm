/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * H2 array_replace function.
 */
public class H2ArrayReplaceFunction extends ArrayReplaceUnnestFunction {

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression oldExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression newExpression = (Expression) sqlAstArguments.get( 2 );
		sqlAppender.append( "case when ");
		arrayExpression.accept( walker );
		sqlAppender.append( " is null then null else coalesce((select array_agg(case when array_get(");
		arrayExpression.accept( walker );
		sqlAppender.append(",i.idx) is not distinct from ");
		oldExpression.accept( walker );
		sqlAppender.append( " then " );
		newExpression.accept( walker );
		sqlAppender.append( " else array_get(" );
		arrayExpression.accept( walker );
		sqlAppender.append(",i.idx) end) from system_range(1," );
		sqlAppender.append( Integer.toString( getMaximumArraySize() ) );
		sqlAppender.append( ") i(idx) where i.idx<=coalesce(cardinality(");
		arrayExpression.accept( walker );
		sqlAppender.append("),0)),array[]) end" );
	}

	protected int getMaximumArraySize() {
		return 1000;
	}
}
