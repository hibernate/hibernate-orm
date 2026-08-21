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
import org.hibernate.type.spi.TypeConfiguration;

/**
 * MySQL json_object function.
 */
public class MySQLJsonObjectFunction extends JsonObjectFunction {

	public MySQLJsonObjectFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( "json_object()" );
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
				nullBehavior = JsonNullBehavior.NULL;
				argumentsCount = sqlAstArguments.size();
			}
			if ( nullBehavior == JsonNullBehavior.ABSENT ) {
				sqlAppender.appendSql( "(select json_objectagg(t.k,t.v) from (select " );
				sqlAstArguments.get( 0 ).accept( walker );
				sqlAppender.appendSql( " k,json_extract(json_array(" );
				sqlAstArguments.get( 1 ).accept( walker );
				sqlAppender.appendSql( "),'$[0]') v" );
				for ( int i = 2; i < argumentsCount; i += 2 ) {
					sqlAppender.appendSql( " union all select " );
					sqlAstArguments.get( i ).accept( walker );
					sqlAppender.appendSql( ",json_extract(json_array(" );
					sqlAstArguments.get( i + 1 ).accept( walker );
					sqlAppender.appendSql( "),'$[0]')" );
				}
				sqlAppender.appendSql( ") t where t.v<>json_extract(json_array(null), '$[0]'))" );
			}
			else {
				sqlAppender.appendSql( "json_object" );
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
