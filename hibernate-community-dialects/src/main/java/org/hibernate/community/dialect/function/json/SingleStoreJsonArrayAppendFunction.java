/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.json.AbstractJsonArrayAppendFunction;
import org.hibernate.dialect.function.json.JsonPathHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.UnparsedNumericLiteral;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SingleStore json_array_append function.
 */
public class SingleStoreJsonArrayAppendFunction extends AbstractJsonArrayAppendFunction {

	public SingleStoreJsonArrayAppendFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final Expression json = (Expression) arguments.get( 0 );
		final Expression jsonPath = (Expression) arguments.get( 1 );
		final List<JsonPathHelper.JsonPathElement> jsonPathElements = JsonPathHelper.parseJsonPathElements( translator.getLiteralValue(
				jsonPath ) );
		final SqlAstNode value = arguments.get( 2 );
		sqlAppender.appendSql( "json_set_json(" );
		json.accept( translator );
		buildJsonPath( sqlAppender, jsonPath, jsonPathElements );
		sqlAppender.appendSql( ',' );
		sqlAppender.appendSql( " case when json_get_type(json_extract_json(" );
		json.accept( translator );
		buildJsonPath( sqlAppender, jsonPath, jsonPathElements );
		sqlAppender.appendSql( ")) = 'array' THEN " );
		buildJsonArrayPushValue( sqlAppender, value );
		sqlAppender.appendSql( "json_extract_json(" );
		json.accept( translator );
		buildJsonPath( sqlAppender, jsonPath, jsonPathElements );
		sqlAppender.appendSql( ")," );
		value.accept( translator );
		sqlAppender.appendSql( ") ELSE " );
		buildJsonArrayPushValue( sqlAppender, value );
		sqlAppender.appendSql( "json_build_array(json_extract_json(" );
		json.accept( translator );
		buildJsonPath( sqlAppender, jsonPath, jsonPathElements );
		sqlAppender.appendSql( "))," );
		value.accept( translator );
		sqlAppender.appendSql( ") END )" );
	}

	private static boolean isNumeric(SqlAstNode value) {
		return value instanceof UnparsedNumericLiteral<?>;
	}

	private static void buildJsonArrayPushValue(SqlAppender sqlAppender, SqlAstNode value) {
		sqlAppender.appendSql( "json_array_push_" );
		sqlAppender.appendSql( isNumeric( value ) ? "double(" : "string(" );
	}

	private static void buildJsonPath(
			SqlAppender sqlAppender, Expression jsonPath, List<JsonPathHelper.JsonPathElement> jsonPathElements) {
		for ( JsonPathHelper.JsonPathElement pathElement : jsonPathElements ) {
			sqlAppender.appendSql( ',' );
			if ( pathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
				sqlAppender.appendSingleQuoteEscapedString( attribute.attribute() );
			}
			else if ( pathElement instanceof JsonPathHelper.JsonParameterIndexAccess ) {
				final String parameterName = ( (JsonPathHelper.JsonParameterIndexAccess) pathElement ).parameterName();
				throw new QueryException( "JSON path [" + jsonPath + "] uses parameter [" + parameterName + "] that is not passed" );
			}
			else {
				sqlAppender.appendSql( '\'' );
				sqlAppender.appendSql( ( (JsonPathHelper.JsonIndexAccess) pathElement ).index() );
				sqlAppender.appendSql( '\'' );
			}
		}
	}
}
