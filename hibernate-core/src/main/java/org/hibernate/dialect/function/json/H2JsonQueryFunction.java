/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * H2 json_query function.
 */
public class H2JsonQueryFunction extends JsonQueryFunction {

	public H2JsonQueryFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false, true );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonQueryArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Json dereference errors by default if the JSON is invalid
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonQueryErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on H2" );
		}
		if ( arguments.emptyBehavior() == JsonQueryEmptyBehavior.ERROR ) {
			throw new QueryException( "Can't emulate error on empty clause on H2" );
		}
		appendJsonQuery(
				sqlAppender,
				arguments.jsonDocument(),
				arguments.isJsonType(),
				arguments.jsonPath(),
				arguments.passingClause(),
				arguments.wrapMode(),
				arguments.emptyBehavior(),
				walker
		);
	}

	static void appendJsonQuery(
			SqlAppender sqlAppender,
			Expression jsonDocument,
			boolean isJsonType,
			Expression jsonPathExpression,
			@Nullable JsonPathPassingClause passingClause,
			@Nullable JsonQueryWrapMode wrapMode,
			@Nullable JsonQueryEmptyBehavior emptyBehavior,
			SqlAstTranslator<?> walker) {
		final String jsonPath;
		try {
			jsonPath = walker.getLiteralValue( jsonPathExpression );
		}
		catch (Exception ex) {
			throw new QueryException( "H2 json_query only support literal json paths, but got " + jsonPathExpression );
		}
		appendJsonQuery( sqlAppender, jsonDocument, isJsonType, jsonPath, passingClause, wrapMode, emptyBehavior, walker );
	}

	static void appendJsonQuery(
			SqlAppender sqlAppender,
			Expression jsonDocument,
			boolean isJsonType,
			String jsonPath,
			@Nullable JsonPathPassingClause passingClause,
			@Nullable JsonQueryWrapMode wrapMode,
			@Nullable JsonQueryEmptyBehavior emptyBehavior,
			SqlAstTranslator<?> walker) {
		if ( emptyBehavior == JsonQueryEmptyBehavior.EMPTY_ARRAY || emptyBehavior == JsonQueryEmptyBehavior.EMPTY_OBJECT ) {
			sqlAppender.appendSql( "coalesce(" );
		}

		if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( "'['||" );
		}

		sqlAppender.appendSql( "stringdecode(regexp_replace(nullif(" );
		H2JsonValueFunction.renderJsonPath(
				sqlAppender,
				jsonDocument,
				isJsonType,
				walker,
				jsonPath,
				passingClause
		);
		sqlAppender.appendSql( ",JSON'null'),'^\"(.*)\"$','$1'))");
		if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( "||']'" );
		}
		if ( emptyBehavior == JsonQueryEmptyBehavior.EMPTY_ARRAY ) {
			sqlAppender.appendSql( ",'[]')" );
		}
		else if ( emptyBehavior == JsonQueryEmptyBehavior.EMPTY_OBJECT ) {
			sqlAppender.appendSql( ",'{}')" );
		}
	}
}
