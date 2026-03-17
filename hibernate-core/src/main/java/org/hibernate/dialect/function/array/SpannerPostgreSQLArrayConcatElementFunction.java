/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * Spanner PostgreSQL variant of the function to properly return {@code null} when the array argument is null
 * without generating casts which Spanner doesn't support.
 */
public class SpannerPostgreSQLArrayConcatElementFunction extends ArrayConcatElementFunction {

	public SpannerPostgreSQLArrayConcatElementFunction(boolean prepend) {
		super( "", "||", "", prepend );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression firstArgument = (Expression) sqlAstArguments.get( 0 );
		final Expression secondArgument = (Expression) sqlAstArguments.get( 1 );
		final Expression arrayArgument;
		final Expression elementArgument;
		if ( prepend ) {
			elementArgument = firstArgument;
			arrayArgument = secondArgument;
		}
		else {
			arrayArgument = firstArgument;
			elementArgument = secondArgument;
		}

		sqlAppender.append( "case when " );
		walker.render( arrayArgument, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.append( " is not null then " );

		if ( prepend ) {
			sqlAppender.append( "array[" );
			walker.render( elementArgument, SqlAstNodeRenderingMode.DEFAULT );
			sqlAppender.append( "]" );
		}
		else {
			walker.render( arrayArgument, SqlAstNodeRenderingMode.DEFAULT );
		}

		sqlAppender.append( "||" );

		if ( prepend ) {
			walker.render( arrayArgument, SqlAstNodeRenderingMode.DEFAULT );
		}
		else {
			sqlAppender.append( "array[" );
			walker.render( elementArgument, SqlAstNodeRenderingMode.DEFAULT );
			sqlAppender.append( "]" );
		}

		sqlAppender.append( " end" );
	}
}
