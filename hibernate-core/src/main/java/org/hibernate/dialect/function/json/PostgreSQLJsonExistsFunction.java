/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.Map;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * PostgreSQL json_exists function.
 */
public class PostgreSQLJsonExistsFunction extends JsonExistsFunction {

	public PostgreSQLJsonExistsFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, true );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonExistsArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "jsonb_path_exists(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ',' );
		arguments.jsonPath().accept( walker );
		final JsonPathPassingClause passingClause = arguments.passingClause();
		if ( passingClause != null ) {
			sqlAppender.append( ",jsonb_build_object" );
			char separator = '(';
			for ( Map.Entry<String, Expression> entry : passingClause.getPassingExpressions().entrySet() ) {
				sqlAppender.append( separator );
				sqlAppender.appendSingleQuoteEscapedString( entry.getKey() );
				sqlAppender.append( ',' );
				entry.getValue().accept( walker );
				separator = ',';
			}
			sqlAppender.append( ')' );
		}
		sqlAppender.appendSql( ')' );
	}
}
