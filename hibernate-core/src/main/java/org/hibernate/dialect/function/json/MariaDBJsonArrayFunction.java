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
 * MariaDB json_array function.
 */
public class MariaDBJsonArrayFunction extends JsonArrayFunction {

	public MariaDBJsonArrayFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.isEmpty() ) {
			sqlAppender.appendSql( "json_array()" );
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
				sqlAppender.appendSql( "(select json_arrayagg(t.v order by t.i) from (select 0 i,json_extract(json_array(" );
				sqlAstArguments.get( 0 ).accept( walker );
				sqlAppender.appendSql( "),'$[0]') v" );
				for ( int i = 1; i < argumentsCount; i++ ) {
					sqlAppender.appendSql( " union all select " );
					sqlAppender.appendSql( i );
					sqlAppender.appendSql( ",json_extract(json_array(" );
					sqlAstArguments.get( i ).accept( walker );
					sqlAppender.appendSql( "),'$[0]')" );
				}
				sqlAppender.appendSql( ") t where t.v is not null)" );
			}
			else {
				sqlAppender.appendSql( "json_array" );
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
