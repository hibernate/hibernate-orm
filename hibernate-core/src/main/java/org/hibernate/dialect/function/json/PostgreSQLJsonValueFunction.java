/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.Map;

import org.hibernate.QueryException;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * PostgreSQL json_value function.
 */
public class PostgreSQLJsonValueFunction extends JsonValueFunction {

	public PostgreSQLJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, true );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// jsonb_path_query_first errors by default
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonValueErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on PostgreSQL" );
		}
		if ( arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on PostgreSQL" );
		}
		if ( arguments.returningType() != null ) {
			sqlAppender.appendSql( "cast(" );
		}
		sqlAppender.appendSql( "jsonb_path_query_first(" );
		final boolean needsCast = !arguments.isJsonType() && arguments.jsonDocument() instanceof JdbcParameter;
		if ( needsCast ) {
			sqlAppender.appendSql( "cast(" );
		}
		arguments.jsonDocument().accept( walker );
		if ( needsCast ) {
			sqlAppender.appendSql( " as jsonb)" );
		}
		sqlAppender.appendSql( ',' );
		final SqlAstNode jsonPath = arguments.jsonPath();
		if ( jsonPath instanceof Literal ) {
			jsonPath.accept( walker );
		}
		else {
			sqlAppender.appendSql( "cast(" );
			jsonPath.accept( walker );
			sqlAppender.appendSql( " as jsonpath)" );
		}
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
		// Unquote the value
		sqlAppender.appendSql( ")#>>'{}'" );
		if ( arguments.returningType() != null ) {
			sqlAppender.appendSql( " as " );
			arguments.returningType().accept( walker );
			sqlAppender.appendSql( ')' );
		}
	}
}
