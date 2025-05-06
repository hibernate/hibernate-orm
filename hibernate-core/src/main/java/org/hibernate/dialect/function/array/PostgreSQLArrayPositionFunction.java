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
import org.hibernate.type.spi.TypeConfiguration;

/**
 * PostgreSQL variant of the function.
 */
public class PostgreSQLArrayPositionFunction extends AbstractArrayPositionFunction {

	public PostgreSQLArrayPositionFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression elementExpression = (Expression) sqlAstArguments.get( 1 );
		sqlAppender.append( "case when " );
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce(array_position(" );
		walker.render( arrayExpression, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.append( ',' );
		walker.render( elementExpression, SqlAstNodeRenderingMode.DEFAULT );
		if ( sqlAstArguments.size() > 2 ) {
			sqlAppender.append( ',' );
			sqlAstArguments.get( 2 ).accept( walker );
		}
		sqlAppender.append( "),0) end" );
	}
}
