/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;

import java.util.List;

/**
 * Implement the array get function by using {@code []} (bracket) syntax.
 */
public class ArrayGetBracketFunction extends AbstractArrayGetFunction {

	private final boolean supportsJsonBracket;

	public ArrayGetBracketFunction(boolean supportsJsonBracket) {
		this.supportsJsonBracket = supportsJsonBracket;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression indexExpression = (Expression) sqlAstArguments.get( 1 );
		final JdbcMappingContainer arrayTypeContainer = arrayExpression.getExpressionType();
		final JdbcMapping arrayType = arrayTypeContainer == null ? null : arrayTypeContainer.getSingleJdbcMapping();
		final boolean isJson = arrayType instanceof BasicPluralType<?, ?> && arrayType.getJdbcType().isJson();
		if ( isJson && !supportsJsonBracket ) {
			// JSON arrays have 0-based indexed, so we have to adapt the 1-based array_get index
			sqlAppender.append( "(select jsonb_path_query(" );
			arrayExpression.accept( walker );
			sqlAppender.append( ",'$[$i]',('{\"i\":'||((" );
			indexExpression.accept( walker );
			sqlAppender.append( ")-1)||'}')::jsonb))" );
		}
		else {
			arrayExpression.accept( walker );
			sqlAppender.append( '[' );
			indexExpression.accept( walker );
			if ( isJson ) {
				// JSON arrays have 0-based indexed, so we have to adapt the 1-based array_get index
				sqlAppender.append( "-1" );
			}
			sqlAppender.append( ']' );
		}
	}
}
