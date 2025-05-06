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
public class H2ArraySetFunction extends ArraySetUnnestFunction {

	private final int maximumArraySize;

	public H2ArraySetFunction(int maximumArraySize) {
		this.maximumArraySize = maximumArraySize;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression indexExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression elementExpression = (Expression) sqlAstArguments.get( 2 );
		sqlAppender.append( "(select array_agg(case when i.idx=");
		indexExpression.accept( walker );
		sqlAppender.append(" then " );
		elementExpression.accept( walker );
		sqlAppender.append(" when " );
		arrayExpression.accept( walker );
		sqlAppender.append(" is not null and i.idx<=cardinality(");
		arrayExpression.accept( walker );
		sqlAppender.append(") then array_get(");
		arrayExpression.accept( walker );
		sqlAppender.append(",i.idx) end) from system_range(1," );
		sqlAppender.append( Integer.toString( maximumArraySize ) );
		sqlAppender.append( ") i(idx) where i.idx<=greatest(case when ");
		arrayExpression.accept( walker );
		sqlAppender.append(" is not null then cardinality(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") else 0 end," );
		indexExpression.accept( walker );
		sqlAppender.append( "))" );
	}
}
