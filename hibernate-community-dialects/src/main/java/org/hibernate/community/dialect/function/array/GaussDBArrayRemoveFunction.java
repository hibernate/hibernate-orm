/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.array;

import org.hibernate.dialect.function.array.AbstractArrayRemoveFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.Expression;

import java.util.List;

import static org.hibernate.sql.ast.spi.query.SqlAstTreeHelper.isNullLiteral;

/**
 * GaussDB array_remove function.
 * @author chenzhida
 */
public class GaussDBArrayRemoveFunction extends AbstractArrayRemoveFunction {

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression valueExpression = (Expression) sqlAstArguments.get( 1 );

		sqlAppender.append( "CASE WHEN ");
		arrayExpression.accept( walker );
		sqlAppender.append( " IS NULL THEN NULL ELSE COALESCE(( SELECT array_agg(val) FROM unnest(");
		arrayExpression.accept( walker );
		sqlAppender.append( ") AS val" );

		if ( isNullLiteral( valueExpression ) ) {
			sqlAppender.append( " where val IS NOT NULL" );
		}
		else {
			appendWhere( sqlAppender, walker, valueExpression );
		}
		sqlAppender.append( "),  CAST(ARRAY[] AS VARCHAR[]) ) END AS result_array" );
	}

	/**
	 * can not get value if type like string
	 * @param sqlAppender
	 * @param walker
	 * @param indexExpression
	 */
	private static void appendWhere(SqlAppender sqlAppender, SqlAstTranslator<?> walker, Expression indexExpression) {
		sqlAppender.append( " where val IS NULL OR val not in (" );
		indexExpression.accept( walker );
		sqlAppender.append( ")" );
	}
}
