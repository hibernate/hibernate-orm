/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import java.util.List;

import org.hibernate.dialect.function.json.JsonArrayFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SingleStore json_array function.
 */
public class SingleStoreJsonArrayFunction extends JsonArrayFunction {

	public SingleStoreJsonArrayFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( "json_build_array()" );
		}
		else {
			final SqlAstNode lastArgument = sqlAstArguments.get( sqlAstArguments.size() - 1 );
			final JsonNullBehavior nullBehavior;
			final int argumentsCount;
			if ( lastArgument instanceof JsonNullBehavior ) {
				nullBehavior = (JsonNullBehavior) lastArgument;
				argumentsCount = sqlAstArguments.size() - 1;
			}
			else {
				nullBehavior = null;
				argumentsCount = sqlAstArguments.size();
			}
			if ( nullBehavior == JsonNullBehavior.ABSENT ) {
				sqlAppender.appendSql( "(select json_agg(t.v order by t.i) from (select 0 i, to_json(" );
				sqlAstArguments.get( 0 ).accept( walker );
				sqlAppender.appendSql( ") v" );
				for ( int i = 1; i < argumentsCount; i++ ) {
					sqlAppender.appendSql( " union all select " );
					sqlAppender.appendSql( i );
					sqlAppender.appendSql( ", to_json(" );
					sqlAstArguments.get( i ).accept( walker );
					sqlAppender.appendSql( ')' );
				}
				sqlAppender.appendSql( ") t where t.v is not null)" );
			}
			else {
				sqlAppender.appendSql( "json_build_array" );
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
