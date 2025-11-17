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
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Oracle json_array_append function.
 */
public class OracleJsonArrayAppendFunction extends AbstractJsonArrayAppendFunction {

	public OracleJsonArrayAppendFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final Expression json = (Expression) arguments.get( 0 );
		final String jsonPath = translator.getLiteralValue( (Expression) arguments.get( 1 ) );
		final SqlAstNode value = arguments.get( 2 );
		sqlAppender.appendSql( "(select case coalesce(json_value(t.d,'" );
		for ( int i = 0; i < jsonPath.length(); i++ ) {
			final char c = jsonPath.charAt( i );
			if ( c == '\'') {
				sqlAppender.appendSql( "'" );
			}
			sqlAppender.appendSql( c );
		}
		sqlAppender.appendSql( ".type()'),'x') when 'x' then t.d when 'array' then json_transform(t.d,append " );
		sqlAppender.appendSingleQuoteEscapedString( jsonPath );
		sqlAppender.appendSql( "=t.v) when 'object' then json_transform(t.d,set " );
		sqlAppender.appendSingleQuoteEscapedString( jsonPath );
		sqlAppender.appendSql( "=json_array(coalesce(json_query(t.d," );
		sqlAppender.appendSingleQuoteEscapedString( jsonPath );
		sqlAppender.appendSql( "),'null') format json,t.v)) else json_transform(t.d,set " );
		sqlAppender.appendSingleQuoteEscapedString( jsonPath );
		sqlAppender.appendSql( "=json_array(coalesce(json_value(t.d," );
		sqlAppender.appendSingleQuoteEscapedString( jsonPath );
		sqlAppender.appendSql( "),'null') format json,t.v)) end from (select " );
		json.accept( translator );
		sqlAppender.appendSql( " d," );
		value.accept( translator );
		sqlAppender.appendSql( " v from dual) t)" );

	}
}
