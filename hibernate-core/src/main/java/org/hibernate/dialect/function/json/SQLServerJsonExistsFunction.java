/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server json_exists function.
 */
public class SQLServerJsonExistsFunction extends JsonExistsFunction {

	private final boolean supportsExtendedJson;

	public SQLServerJsonExistsFunction(boolean supportsExtendedJson, TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
		this.supportsExtendedJson = supportsExtendedJson;
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonExistsArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( arguments.errorBehavior() == JsonExistsErrorBehavior.TRUE ) {
			throw new QueryException( "Can't emulate json_exists(... true on error) on SQL Server" );
		}
		if ( supportsExtendedJson ) {
			if ( arguments.errorBehavior() == JsonExistsErrorBehavior.ERROR ) {
				sqlAppender.append( '(' );
			}
			sqlAppender.appendSql( "json_path_exists(" );
			arguments.jsonDocument().accept( walker );
			sqlAppender.appendSql( ',' );
			final JsonPathPassingClause passingClause = arguments.passingClause();
			if ( passingClause != null ) {
				JsonPathHelper.appendInlinedJsonPathIncludingPassingClause(
						sqlAppender,
						"",
						arguments.jsonPath(),
						passingClause,
						walker
				);
			}
			else {
				walker.getSessionFactory().getJdbcServices().getDialect().appendLiteral(
						sqlAppender,
						walker.getLiteralValue( arguments.jsonPath() )
				);
			}
			sqlAppender.appendSql( ")=1" );
			if ( arguments.errorBehavior() == JsonExistsErrorBehavior.ERROR ) {
				// json_path_exists returns 0 if an invalid JSON is given,
				// so we have to run openjson to be sure the json is valid and potentially throw an error
				sqlAppender.appendSql( " or (select v from openjson(" );
				arguments.jsonDocument().accept( walker );
				sqlAppender.appendSql( ") with (v varchar(max) " );
				if ( passingClause != null ) {
					JsonPathHelper.appendInlinedJsonPathIncludingPassingClause(
							sqlAppender,
							"",
							arguments.jsonPath(),
							passingClause,
							walker
					);
				}
				else {
					walker.getSessionFactory().getJdbcServices().getDialect().appendLiteral(
							sqlAppender,
							walker.getLiteralValue( arguments.jsonPath() )
					);
				}
				sqlAppender.appendSql( ")) is null)" );
			}
		}
		else {
			if ( arguments.errorBehavior() == JsonExistsErrorBehavior.FALSE ) {
				throw new QueryException( "Can't emulate json_exists(... false on error) on SQL Server" );
			}
			final String jsonPath = walker.getLiteralValue( arguments.jsonPath() );
			final JsonPathPassingClause passingClause = arguments.passingClause();
			final List<JsonPathHelper.JsonPathElement> pathElements = JsonPathHelper.parseJsonPathElements( jsonPath );
			if ( passingClause != null ) {
				JsonPathHelper.inlinePassingClause( pathElements, passingClause, walker );
			}
			final JsonPathHelper.JsonPathElement lastPathElement = pathElements.get( pathElements.size() - 1 );
			final String prefix = JsonPathHelper.toJsonPath( pathElements, 0, pathElements.size() - 1 );
			final String terminalKey;
			if ( lastPathElement instanceof JsonPathHelper.JsonIndexAccess indexAccess ) {
				terminalKey = String.valueOf( indexAccess.index() );
			}
			else if (lastPathElement instanceof JsonPathHelper.JsonAttribute attribute) {
				terminalKey = attribute.attribute();
			}
			else {
				throw new AssertionFailure( "Unrecognized json path element: " + lastPathElement );
			}

			sqlAppender.appendSql( "(select 1 from openjson(" );
			arguments.jsonDocument().accept( walker );
			sqlAppender.appendSql( ',' );
			sqlAppender.appendSingleQuoteEscapedString( prefix );
			sqlAppender.appendSql( ") t where t.[key]=" );
			sqlAppender.appendSingleQuoteEscapedString( terminalKey );
			sqlAppender.appendSql( ")=1" );
		}
	}
}
