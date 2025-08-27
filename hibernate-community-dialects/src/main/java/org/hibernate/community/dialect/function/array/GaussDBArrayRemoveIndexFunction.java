/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.array;

import org.hibernate.dialect.function.array.ArrayRemoveIndexUnnestFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;

import java.util.List;

/**
 * GaussDB array_remove index function.
 * @author chenzhida
 */
public class GaussDBArrayRemoveIndexFunction extends ArrayRemoveIndexUnnestFunction {

	private final boolean castEmptyArrayLiteral;

	public GaussDBArrayRemoveIndexFunction(boolean castEmptyArrayLiteral) {
		super( castEmptyArrayLiteral );
		this.castEmptyArrayLiteral = castEmptyArrayLiteral;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression indexExpression = (Expression) sqlAstArguments.get( 1 );

		sqlAppender.append( "case when ");
		arrayExpression.accept( walker );
		sqlAppender.append( " IS NOT NULL THEN COALESCE((SELECT array_agg(" );
		arrayExpression.accept( walker );
		sqlAppender.append( "[idx]) FROM generate_subscripts(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ", 1) AS idx " );

		if ( indexExpression instanceof Literal ) {
			Literal literal = (Literal) indexExpression;
			Object literalValue = literal.getLiteralValue();
			if ( literalValue != null ) {
				appendWhere( sqlAppender, walker, indexExpression );
			}
		}
		else {
			appendWhere( sqlAppender, walker, indexExpression );
		}

		sqlAppender.append( "), CAST(ARRAY[] AS VARCHAR ARRAY)) " );
		if ( castEmptyArrayLiteral ) {
			sqlAppender.append( "ELSE CAST(ARRAY[] AS VARCHAR ARRAY) " );
		}
		sqlAppender.append( "END AS result_array" );
	}

	private static void appendWhere(SqlAppender sqlAppender, SqlAstTranslator<?> walker, Expression indexExpression) {
		sqlAppender.append( "where idx not in (" );
		indexExpression.accept( walker );
		sqlAppender.append( ")" );
	}
}
