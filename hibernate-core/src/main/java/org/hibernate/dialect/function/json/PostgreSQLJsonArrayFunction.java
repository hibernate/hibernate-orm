/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * PostgreSQL json_array function.
 */
public class PostgreSQLJsonArrayFunction extends JsonArrayFunction {

	public PostgreSQLJsonArrayFunction(TypeConfiguration typeConfiguration) {
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
				sqlAppender.appendSql( "(select jsonb_agg(t.v order by t.i) from (values" );
				char separator = ' ';
				for ( int i = 0; i < argumentsCount; i++ ) {
					final SqlAstNode node = sqlAstArguments.get( i );
					sqlAppender.appendSql( separator );
					sqlAppender.appendSql( '(' );
					sqlAppender.appendSql( i );
					sqlAppender.appendSql( ',' );
					if ( node instanceof Literal literal && literal.getLiteralValue() == null ) {
						sqlAppender.appendSql( "null::jsonb" );
					}
					else {
						sqlAppender.appendSql( "to_jsonb(" );
						node.accept( walker );
						if ( node instanceof Literal literal && literal.getJdbcMapping().getJdbcType().isString() ) {
							// PostgreSQL until version 16 is not smart enough to infer the type of a string literal
							sqlAppender.appendSql( "::text" );
						}
						sqlAppender.appendSql( ')' );
					}
					sqlAppender.appendSql( ')' );
					separator = ',';
				}
				sqlAppender.appendSql( ") t(i,v) where t.v is not null)" );
			}
			else {
				sqlAppender.appendSql( "jsonb_build_array" );
				char separator = '(';
				for ( int i = 0; i < argumentsCount; i++ ) {
					sqlAppender.appendSql( separator );
					sqlAstArguments.get( i ).accept( walker );
					separator = ',';
				}
				sqlAppender.appendSql( ')' );
			}
		}
	}
}
