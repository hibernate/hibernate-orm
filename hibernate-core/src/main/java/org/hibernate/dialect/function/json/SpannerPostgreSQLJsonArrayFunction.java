/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.dialect.function.json.JsonArrayFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Spanner PostgreSQL json_array function.
 */
public class SpannerPostgreSQLJsonArrayFunction extends JsonArrayFunction {

	public SpannerPostgreSQLJsonArrayFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( "jsonb_build_array()" );
		}
		else {
			final SqlAstNode lastArgument = sqlAstArguments.get( sqlAstArguments.size() - 1 );
			final JsonNullBehavior nullBehavior;
			final int argumentsCount;
			if ( lastArgument instanceof JsonNullBehavior jsonNullBehavior ) {
				nullBehavior = jsonNullBehavior;
				argumentsCount = sqlAstArguments.size() - 1;
			}
			else {
				nullBehavior = null;
				argumentsCount = sqlAstArguments.size();
			}

			if ( nullBehavior == JsonNullBehavior.ABSENT ) {
				sqlAppender.appendSql( "jsonb_strip_nulls(jsonb_build_array" );
			}
			else {
				sqlAppender.appendSql( "jsonb_build_array" );
			}

			char separator = '(';
			for ( int i = 0; i < argumentsCount; i++ ) {
				sqlAppender.appendSql( separator );
				sqlAstArguments.get( i ).accept( walker );
				separator = ',';
			}
			sqlAppender.appendSql( ')' );

			if ( nullBehavior == JsonNullBehavior.ABSENT ) {
				sqlAppender.appendSql( ')' );
			}
		}
	}
}
