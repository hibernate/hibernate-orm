/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * H2 json_value function.
 */
public class H2JsonValueFunction extends JsonValueFunction {

	public H2JsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false, true );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Json dereference errors by default if the JSON is invalid
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonValueErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on H2" );
		}
		if ( arguments.emptyBehavior() == JsonValueEmptyBehavior.ERROR ) {
			throw new QueryException( "Can't emulate error on empty clause on H2" );
		}
		final Expression defaultExpression = arguments.emptyBehavior() == null
				? null
				: arguments.emptyBehavior().getDefaultExpression();
		if ( arguments.returningType() != null ) {
			sqlAppender.appendSql( "cast(" );
		}
		final String jsonPath;
		try {
			jsonPath = walker.getLiteralValue( arguments.jsonPath() );
		}
		catch (Exception ex) {
			throw new QueryException( "H2 json_value only support literal json paths, but got " + arguments.jsonPath() );
		}

		sqlAppender.appendSql( "stringdecode(btrim(nullif(" );
		if ( defaultExpression != null ) {
			sqlAppender.appendSql( "coalesce(" );
		}
		sqlAppender.appendSql( "cast(" );
		renderJsonPath(
				sqlAppender,
				arguments.jsonDocument(),
				arguments.isJsonType(),
				walker,
				jsonPath,
				arguments.passingClause()
		);
		sqlAppender.appendSql( " as varchar)" );
		if ( defaultExpression != null ) {
			sqlAppender.appendSql( ",cast(" );
			defaultExpression.accept( walker );
			sqlAppender.appendSql( " as varchar))" );
		}
		sqlAppender.appendSql( ",'null'),'\"'))");

		if ( arguments.returningType() != null ) {
			sqlAppender.appendSql( " as " );
			arguments.returningType().accept( walker );
			sqlAppender.appendSql( ')' );
		}
	}

	public static void renderJsonPath(
			SqlAppender sqlAppender,
			Expression jsonDocument,
			boolean isJson,
			SqlAstTranslator<?> walker,
			String jsonPath,
			@Nullable JsonPathPassingClause passingClause) {
		final List<JsonPathHelper.JsonPathElement> jsonPathElements = JsonPathHelper.parseJsonPathElements( jsonPath );
		final boolean needsWrapping = jsonPathElements.get( 0 ) instanceof JsonPathHelper.JsonAttribute
				&& jsonDocument.getColumnReference() != null
				|| !isJson;
		if ( needsWrapping ) {
			sqlAppender.appendSql( '(' );
		}
		jsonDocument.accept( walker );
		if ( needsWrapping ) {
			if ( !isJson ) {
				sqlAppender.append( " format json" );
			}
			sqlAppender.appendSql( ')' );
		}
		for ( int i = 0; i < jsonPathElements.size(); i++ ) {
			final JsonPathHelper.JsonPathElement jsonPathElement = jsonPathElements.get( i );
			if ( jsonPathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
				final String attributeName = attribute.attribute();
				sqlAppender.appendSql( "." );
				sqlAppender.appendDoubleQuoteEscapedString( attributeName );
			}
			else if ( jsonPathElement instanceof JsonPathHelper.JsonParameterIndexAccess ) {
				assert passingClause != null;
				final String parameterName = ( (JsonPathHelper.JsonParameterIndexAccess) jsonPathElement ).parameterName();
				final Expression expression = passingClause.getPassingExpressions().get( parameterName );
				if ( expression == null ) {
					throw new QueryException( "JSON path [" + jsonPath + "] uses parameter [" + parameterName + "] that is not passed" );
				}

				sqlAppender.appendSql( '[' );
				expression.accept( walker );
				sqlAppender.appendSql( "+1]" );
			}
			else {
				sqlAppender.appendSql( '[' );
				sqlAppender.appendSql( ( (JsonPathHelper.JsonIndexAccess) jsonPathElement ).index() + 1 );
				sqlAppender.appendSql( ']' );
			}
		}
	}
}
