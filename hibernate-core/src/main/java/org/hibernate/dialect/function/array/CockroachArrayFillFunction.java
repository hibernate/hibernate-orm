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
import org.hibernate.sql.ast.tree.expression.Literal;

/**
 * Implement the array fill function by using {@code generate_series}.
 */
public class CockroachArrayFillFunction extends AbstractArrayFillFunction {

	public CockroachArrayFillFunction(boolean list) {
		super( list );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.append( "coalesce(case when " );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( "<>0 then (select array_agg(" );
		final String elementCastType;
		final Expression elementExpression = (Expression) sqlAstArguments.get( 0 );
		if ( needsElementCasting( elementExpression ) ) {
			elementCastType = DdlTypeHelper.getCastTypeName(
					elementExpression.getExpressionType(),
					walker.getSessionFactory().getTypeConfiguration()
			);
			sqlAppender.append( "cast(" );
		}
		else {
			elementCastType = null;
		}
		sqlAstArguments.get( 0 ).accept( walker );
		if ( elementCastType != null ) {
			sqlAppender.append( " as " );
			sqlAppender.append( elementCastType );
			sqlAppender.append( ')' );
		}
		sqlAppender.append( ") from generate_series(1," );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( ",1)) end,array[])" );
	}

	private static boolean needsElementCasting(Expression elementExpression) {
		// PostgreSQL needs casting of null and string literal expressions
		return elementExpression instanceof Literal literal
			&& ( elementExpression.getExpressionType().getSingleJdbcMapping().getJdbcType().isString()
					|| literal.getLiteralValue() == null );
	}
}
