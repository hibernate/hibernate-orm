/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
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
		final String jsonPath;
		try {
			jsonPath = walker.getLiteralValue( arguments.jsonPath() );
		}
		catch (Exception ex) {
			throw new QueryException( "H2 json_query only support literal json paths, but got " + arguments.jsonPath() );
		}
		if ( arguments.wrapMode() == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( "'['||" );
		}

		sqlAppender.appendSql( "stringdecode(btrim(nullif(" );
		sqlAppender.appendSql( "cast(" );
		H2JsonValueFunction.renderJsonPath(
				sqlAppender,
				arguments.jsonDocument(),
				arguments.isJsonType(),
				walker,
				jsonPath,
				arguments.passingClause()
		);
		sqlAppender.appendSql( " as varchar)" );
		sqlAppender.appendSql( ",'null'),'\"'))");
		if ( arguments.wrapMode() == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( "||']'" );
		}
	}
}
