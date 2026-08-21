/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server json_value function.
 */
public class SQLServerJsonValueFunction extends JsonValueFunction {

	public SQLServerJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// openjson errors by default
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonValueErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on SQL server" );
		}
		sqlAppender.appendSql( "(select " );
		final CastTarget returningType = arguments.returningType();
		if ( returningType != null && returningType.getJdbcMapping().getJdbcType().isBinary() ) {
			sqlAppender.appendSql( "convert(" );
			returningType.accept( walker );
			sqlAppender.appendSql( ",v,2)" );
		}
		else {
			sqlAppender.appendSql( "v" );
		}
		sqlAppender.appendSql( " from openjson(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ") with (v " );
		if ( returningType != null ) {
			if ( returningType.getJdbcMapping().getJdbcType().isBinary() ) {
				sqlAppender.appendSql( "varchar(max)" );
			}
			else {
				returningType.accept( walker );
			}
		}
		else {
			sqlAppender.appendSql( "nvarchar(max)" );
		}
		sqlAppender.appendSql( ' ' );
		final JsonPathPassingClause passingClause = arguments.passingClause();
		if ( arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonValueEmptyBehavior.NULL ) {
			// The strict modifier will cause an error to be thrown if a field doesn't exist
			if ( passingClause != null ) {
				JsonPathHelper.appendInlinedJsonPathIncludingPassingClause(
						sqlAppender,
						"strict ",
						arguments.jsonPath(),
						passingClause,
						walker
				);
			}
			else {
				walker.getSessionFactory().getJdbcServices().getDialect().appendLiteral(
						sqlAppender,
						"strict " + walker.getLiteralValue( arguments.jsonPath() )
				);
			}
		}
		else if ( passingClause != null ) {
			JsonPathHelper.appendInlinedJsonPathIncludingPassingClause(
					sqlAppender,
					"",
					arguments.jsonPath(),
					passingClause,
					walker
			);
		}
		else {
			arguments.jsonPath().accept( walker );
		}
		sqlAppender.appendSql( "))" );
	}
}
