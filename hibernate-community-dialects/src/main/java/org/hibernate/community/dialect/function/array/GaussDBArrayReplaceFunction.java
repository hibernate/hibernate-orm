/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.array;

import org.hibernate.dialect.function.array.ArrayReplaceUnnestFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;

import java.util.List;

/**
 * GaussDB array_replace function.
 * @author chenzhida
 */
public class GaussDBArrayReplaceFunction extends ArrayReplaceUnnestFunction {

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		sqlAppender.append( "CASE WHEN ");
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.append( " IS NULL THEN NULL ELSE COALESCE((SELECT array_agg(CASE ");
		final Expression originValueExpression = (Expression) sqlAstArguments.get( 1 );
		if ( originValueExpression instanceof Literal ) {
			Literal literal = (Literal) originValueExpression;
			Object literalValue = literal.getLiteralValue();
			if ( literalValue != null ) {
				sqlAppender.append( "WHEN val =  ");
				sqlAstArguments.get( 1 ).accept( walker );
			}
			else {
				sqlAppender.append( "WHEN val is null  ");
			}
		}
		else {
			sqlAppender.append( "WHEN val =  ");
			sqlAstArguments.get( 1 ).accept( walker );
		}
		sqlAppender.append( " THEN  ");
		sqlAstArguments.get( 2 ).accept( walker );
		sqlAppender.append( " ELSE val END) FROM unnest( ");
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.append( ") AS val ), CAST(ARRAY[] AS VARCHAR[]) ) END AS result_array");
	}
}
