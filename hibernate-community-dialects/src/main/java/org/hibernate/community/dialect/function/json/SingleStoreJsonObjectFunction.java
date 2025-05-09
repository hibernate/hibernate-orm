/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import java.util.List;

import org.hibernate.dialect.function.json.JsonObjectFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SingleStore json_object function.
 */
public class SingleStoreJsonObjectFunction extends JsonObjectFunction {

	public SingleStoreJsonObjectFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( "json_build_object()" );
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
				nullBehavior = JsonNullBehavior.NULL;
				argumentsCount = sqlAstArguments.size();
			}
			if ( nullBehavior == JsonNullBehavior.ABSENT ) {
				sqlAppender.appendSql( "(select concat('{', group_concat(concat(t.k, ':', t.v)), '}') from (select " );
				sqlAppender.appendSql( "to_json(" );
				sqlAstArguments.get( 0 ).accept( walker );
				sqlAppender.appendSql( ") k, " );
				sqlAppender.appendSql( "to_json(" );
				sqlAstArguments.get( 1 ).accept( walker );
				sqlAppender.appendSql( ") v" );
				for ( int i = 2; i < argumentsCount; i += 2 ) {
					sqlAppender.appendSql( " union all select " );
					sqlAppender.appendSql( "to_json(" );
					sqlAstArguments.get( i ).accept( walker );
					sqlAppender.appendSql( ")," );
					sqlAppender.appendSql( "to_json(" );
					sqlAstArguments.get( i + 1 ).accept( walker );
					sqlAppender.appendSql( ")" );
				}
				sqlAppender.appendSql( ") t where t.v <> to_json(null))" );
			}
			else {
				sqlAppender.appendSql( "json_build_object" );
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
