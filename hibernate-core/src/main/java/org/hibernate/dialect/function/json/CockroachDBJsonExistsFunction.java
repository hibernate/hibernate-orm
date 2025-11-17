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
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * CockroachDB json_exists function.
 */
public class CockroachDBJsonExistsFunction extends JsonExistsFunction {

	public CockroachDBJsonExistsFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, true );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonExistsArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// jsonb_path_exists errors by default
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonExistsErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on CockroachDB" );
		}
		final String jsonPath;
		try {
			jsonPath = walker.getLiteralValue( arguments.jsonPath() );
		}
		catch (Exception ex) {
			throw new QueryException( "CockroachDB json_value only support literal json paths, but got " + arguments.jsonPath() );
		}
		appendJsonExists(
				sqlAppender,
				arguments.jsonDocument(),
				JsonPathHelper.parseJsonPathElements( jsonPath ),
				arguments.isJsonType(),
				arguments.passingClause(),
				walker
		);
	}

	static void appendJsonExists(
			SqlAppender sqlAppender,
			Expression jsonDocument,
			List<JsonPathHelper.JsonPathElement> jsonPathElements,
			boolean isJsonType,
			@Nullable JsonPathPassingClause jsonPathPassingClause,
			SqlAstTranslator<?> walker) {
		final boolean needsCast = !isJsonType && AbstractSqlAstTranslator.isParameter( jsonDocument );
		if ( needsCast ) {
			sqlAppender.appendSql( "cast(" );
		}
		else {
			sqlAppender.appendSql( '(' );
		}
		jsonDocument.accept( walker );
		if ( needsCast ) {
			sqlAppender.appendSql( " as jsonb)" );
		}
		else {
			sqlAppender.appendSql( ')' );
		}
		sqlAppender.appendSql( "#>>array" );
		char separator = '[';
		final Dialect dialect = walker.getSessionFactory().getJdbcServices().getDialect();
		for ( JsonPathHelper.JsonPathElement jsonPathElement : jsonPathElements ) {
			sqlAppender.appendSql( separator );
			if ( jsonPathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
				dialect.appendLiteral( sqlAppender, attribute.attribute() );
			}
			else if ( jsonPathElement instanceof JsonPathHelper.JsonParameterIndexAccess ) {
				assert jsonPathPassingClause != null;
				final String parameterName = ( (JsonPathHelper.JsonParameterIndexAccess) jsonPathElement ).parameterName();
				final Expression expression = jsonPathPassingClause.getPassingExpressions().get( parameterName );
				if ( expression == null ) {
					throw new QueryException( "JSON path [" + JsonPathHelper.toJsonPath( jsonPathElements ) + "] uses parameter [" + parameterName + "] that is not passed" );
				}

				sqlAppender.appendSql( "cast(" );
				expression.accept( walker );
				sqlAppender.appendSql( " as text)" );
			}
			else {
				sqlAppender.appendSql( '\'' );
				sqlAppender.appendSql( ( (JsonPathHelper.JsonIndexAccess) jsonPathElement ).index() );
				sqlAppender.appendSql( '\'' );
			}
			separator = ',';
		}
		sqlAppender.appendSql( "] is not null" );
	}
}
