/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

import java.util.List;

public class SpannerPostgreSQLTruncRoundFunction extends PostgreSQLTruncRoundFunction {

	public SpannerPostgreSQLTruncRoundFunction() {
		super( "trunc", false );
	}

	@Override
	public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> arguments, ReturnableType<?> returnType, SqlAstTranslator<?> walker) {
		final int numberOfArguments = arguments.size();
		final Expression firstArg = (Expression) arguments.get( 0 );
		if ( numberOfArguments == 1 ) {
			sqlAppender.appendSql( getName() );
			sqlAppender.appendSql( "(" );
			firstArg.accept( walker );
			sqlAppender.appendSql( "::float8" );
			sqlAppender.appendSql( ")" );
		}
		else {
			final SqlAstNode secondArg = arguments.get( 1 );
			sqlAppender.appendSql( "trunc(cast((" );
			firstArg.accept( walker );
			sqlAppender.appendSql( ") as numeric), cast(" );
			secondArg.accept( walker );
			sqlAppender.appendSql( " as integer))");
		}
	}
}
