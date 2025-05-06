/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * CockroachDB json_query function.
 */
public class CockroachDBJsonQueryFunction extends JsonQueryFunction {

	public CockroachDBJsonQueryFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, true );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonQueryArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// jsonb_path_query functions error by default
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonQueryErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on CockroachDB" );
		}
		if ( arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonQueryEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on CockroachDB" );
		}
		final JsonQueryWrapMode wrapMode = arguments.wrapMode();

		if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( "jsonb_build_array(" );
		}
		final Expression jsonDocumentExpression = arguments.jsonDocument();
		final String jsonPath;
		try {
			jsonPath = walker.getLiteralValue( arguments.jsonPath() );
		}
		catch (Exception ex) {
			throw new QueryException( "CockroachDB json_value only support literal json paths, but got " + arguments.jsonPath() );
		}
		appendJsonQuery(
				sqlAppender,
				jsonDocumentExpression,
				JsonPathHelper.parseJsonPathElements( jsonPath ),
				arguments.isJsonType(),
				arguments.passingClause(),
				walker
		);

		if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( ")" );
		}
	}

	static void appendJsonQuery(
			SqlAppender sqlAppender,
			Expression jsonDocumentExpression,
			List<JsonPathHelper.JsonPathElement> jsonPathElements,
			boolean isJsonType,
			@Nullable JsonPathPassingClause jsonPathPassingClause,
			SqlAstTranslator<?> walker) {
		final boolean needsCast = !isJsonType && AbstractSqlAstTranslator.isParameter( jsonDocumentExpression );
		if ( needsCast ) {
			sqlAppender.appendSql( "cast(" );
		}
		else {
			sqlAppender.appendSql( '(' );
		}
		jsonDocumentExpression.accept( walker );
		if ( needsCast ) {
			sqlAppender.appendSql( " as jsonb)" );
		}
		else {
			sqlAppender.appendSql( ')' );
		}
		if ( !jsonPathElements.isEmpty() ) {
			sqlAppender.appendSql( "#>array" );
			char separator = '[';
			final Dialect dialect = walker.getSessionFactory().getJdbcServices().getDialect();
			for ( JsonPathHelper.JsonPathElement jsonPathElement : jsonPathElements ) {
				sqlAppender.appendSql( separator );
				if ( jsonPathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
					dialect.appendLiteral( sqlAppender, attribute.attribute() );
				}
				else if ( jsonPathElement instanceof JsonPathHelper.JsonParameterIndexAccess ) {
					assert jsonPathPassingClause != null;
					final String parameterName = ((JsonPathHelper.JsonParameterIndexAccess) jsonPathElement).parameterName();
					final Expression expression = jsonPathPassingClause.getPassingExpressions().get( parameterName );
					if ( expression == null ) {
						throw new QueryException(
								"JSON path [" + JsonPathHelper.toJsonPath( jsonPathElements ) + "] uses parameter [" + parameterName + "] that is not passed" );
					}

					sqlAppender.appendSql( "cast(" );
					expression.accept( walker );
					sqlAppender.appendSql( " as text)" );
				}
				else {
					sqlAppender.appendSql( '\'' );
					sqlAppender.appendSql( ((JsonPathHelper.JsonIndexAccess) jsonPathElement).index() );
					sqlAppender.appendSql( '\'' );
				}
				separator = ',';
			}
			sqlAppender.appendSql( ']' );
		}
	}
}
