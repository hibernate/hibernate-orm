/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * CockroachDB json_value function.
 */
public class CockroachDBJsonValueFunction extends JsonValueFunction {

	public CockroachDBJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// jsonb_path_query_first errors by default
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonValueErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on CockroachDB" );
		}
		if ( arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on CockroachDB" );
		}
		final String jsonPath;
		try {
			jsonPath = walker.getLiteralValue( arguments.jsonPath() );
		}
		catch (Exception ex) {
			throw new QueryException( "CockroachDB json_value only support literal json paths, but got " + arguments.jsonPath() );
		}
		appendJsonValue(
				sqlAppender,
				arguments.jsonDocument(),
				JsonPathHelper.parseJsonPathElements( jsonPath ),
				arguments.isJsonType(),
				arguments.passingClause(),
				arguments.returningType(),
				walker
		);
	}

	static void appendJsonValue(SqlAppender sqlAppender, Expression jsonDocument, List<JsonPathHelper.JsonPathElement> jsonPathElements, boolean isJsonType, JsonPathPassingClause jsonPathPassingClause, CastTarget castTarget, SqlAstTranslator<?> walker) {
		if ( castTarget != null ) {
			sqlAppender.appendSql( "cast(" );
		}
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
		if ( jsonPathElements.isEmpty() ) {
			sqlAppender.appendSql( '[' );
		}
		sqlAppender.appendSql( ']' );

		if ( castTarget != null ) {
			sqlAppender.appendSql( " as " );
			castTarget.accept( walker );
			sqlAppender.appendSql( ')' );
		}
	}
}
