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
import org.hibernate.sql.ast.tree.expression.Literal;

/**
 * PostgreSQL array_sort emulation for versions before 18.
 */
public class PostgreSQLArraySortEmulation extends AbstractArraySortFunction {

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		final SqlAstNode arrayExpression = sqlAstArguments.get( 0 );

		final boolean descending = sqlAstArguments.size() > 1
				&& sqlAstArguments.get( 1 ) instanceof Literal literal
				&& literal.getLiteralValue() instanceof Boolean boolValue
				? boolValue
				: false;

		final Boolean nullsFirst = sqlAstArguments.size() > 2
				&& sqlAstArguments.get( 2 ) instanceof Literal literal
				&& literal.getLiteralValue() instanceof Boolean boolValue
				? boolValue
				: null;

		final boolean actualNullsFirst = nullsFirst != null ? nullsFirst : descending;

		sqlAppender.append( "coalesce((select array_agg(t.val order by t.val" );

		sqlAppender.append( descending
									? ( actualNullsFirst ? " desc nulls first" : " desc nulls last" )
									: ( actualNullsFirst ? " asc nulls first" : " asc nulls last" ) );

		sqlAppender.append( ") from unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") with ordinality t(val,idx))," );

		arrayExpression.accept( walker );
		sqlAppender.append( ")" );
	}
}
