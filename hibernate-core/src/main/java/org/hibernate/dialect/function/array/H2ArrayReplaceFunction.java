/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * H2 requires a very special emulation, because {@code unnest} is pretty much useless,
 * due to https://github.com/h2database/h2database/issues/1815.
 * This emulation uses {@code array_get}, {@code array_length} and {@code system_range} functions to roughly achieve the same.
 */
public class H2ArrayReplaceFunction extends ArrayReplaceUnnestFunction {

	private final int maximumArraySize;

	public H2ArrayReplaceFunction(int maximumArraySize) {
		this.maximumArraySize = maximumArraySize;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression oldExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression newExpression = (Expression) sqlAstArguments.get( 2 );
		sqlAppender.append( "case when ");
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce((select array_agg(case when array_get(");
		arrayExpression.accept( walker );
		sqlAppender.append(",i.idx) is not distinct from ");
		oldExpression.accept( walker );
		sqlAppender.append( " then " );
		newExpression.accept( walker );
		sqlAppender.append( " else array_get(" );
		arrayExpression.accept( walker );
		sqlAppender.append(",i.idx) end) from system_range(1," );
		sqlAppender.append( Integer.toString( maximumArraySize ) );
		sqlAppender.append( ") i(idx) where i.idx<=coalesce(cardinality(");
		arrayExpression.accept( walker );
		sqlAppender.append("),0)),array[]) end" );
	}
}
