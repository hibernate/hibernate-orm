/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
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
			throw new QueryException( "Can't emulate on error clause on PostgreSQL" );
		}
		if ( arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonQueryEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on PostgreSQL" );
		}
		final JsonQueryWrapMode wrapMode = arguments.wrapMode();

		if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( "jsonb_build_array(" );
		}
		final String jsonPath;
		try {
			jsonPath = walker.getLiteralValue( arguments.jsonPath() );
		}
		catch (Exception ex) {
			throw new QueryException( "CockroachDB json_value only support literal json paths, but got " + arguments.jsonPath() );
		}
		final List<JsonPathHelper.JsonPathElement> jsonPathElements = JsonPathHelper.parseJsonPathElements( jsonPath );
		final boolean needsCast = !arguments.isJsonType() && arguments.jsonDocument() instanceof JdbcParameter;
		if ( needsCast ) {
			sqlAppender.appendSql( "cast(" );
		}
		else {
			sqlAppender.appendSql( '(' );
		}
		arguments.jsonDocument().accept( walker );
		if ( needsCast ) {
			sqlAppender.appendSql( " as jsonb)" );
		}
		else {
			sqlAppender.appendSql( ')' );
		}
		sqlAppender.appendSql( "#>array" );
		char separator = '[';
		final Dialect dialect = walker.getSessionFactory().getJdbcServices().getDialect();
		for ( JsonPathHelper.JsonPathElement jsonPathElement : jsonPathElements ) {
			sqlAppender.appendSql( separator );
			if ( jsonPathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
				dialect.appendLiteral( sqlAppender, attribute.attribute() );
			}
			else if ( jsonPathElement instanceof JsonPathHelper.JsonParameterIndexAccess ) {
				final JsonPathPassingClause jsonPathPassingClause = arguments.passingClause();
				assert jsonPathPassingClause != null;
				final String parameterName = ( (JsonPathHelper.JsonParameterIndexAccess) jsonPathElement ).parameterName();
				final Expression expression = jsonPathPassingClause.getPassingExpressions().get( parameterName );
				if ( expression == null ) {
					throw new QueryException( "JSON path [" + jsonPath + "] uses parameter [" + parameterName + "] that is not passed" );
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
		sqlAppender.appendSql( ']' );

		if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( ")" );
		}
	}
}
