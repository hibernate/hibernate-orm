/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server json_exists function.
 */
public class SQLServerJsonExistsFunction extends JsonExistsFunction {

	public SQLServerJsonExistsFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	public boolean isPredicate() {
		return false;
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
		sqlAppender.appendSql( ')' );
		if ( arguments.errorBehavior() == JsonExistsErrorBehavior.ERROR ) {
			// json_path_exists returns 0 if an invalid JSON is given,
			// so we have to run openjson to be sure the json is valid and potentially throw an error
			sqlAppender.appendSql( "=1 or (select v from openjson(" );
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
}
