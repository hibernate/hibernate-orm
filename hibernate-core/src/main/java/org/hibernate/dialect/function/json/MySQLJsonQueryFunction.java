/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * MySQL json_query function.
 */
public class MySQLJsonQueryFunction extends JsonQueryFunction {

	public MySQLJsonQueryFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonQueryArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// json_extract errors by default
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonQueryErrorBehavior.ERROR
				|| arguments.emptyBehavior() == JsonQueryEmptyBehavior.ERROR
				// Can't emulate DEFAULT ON EMPTY since we can't differentiate between a NULL value and EMPTY
				|| arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonQueryEmptyBehavior.NULL ) {
			super.render( sqlAppender, arguments, returnType, walker );
		}
		else {
			final JsonQueryWrapMode wrapMode = arguments.wrapMode();
			final DecorationMode decorationMode = determineDecorationMode( arguments, walker, wrapMode );
			if ( decorationMode == DecorationMode.WRAP ) {
				sqlAppender.appendSql( "concat('['," );
			}
			else if ( decorationMode == DecorationMode.TRIM ) {
				sqlAppender.appendSql( "cast(trim(leading '[' from trim(trailing ']' from " );
			}

			sqlAppender.appendSql( "nullif(json_extract(" );
			arguments.jsonDocument().accept( walker );
			sqlAppender.appendSql( "," );
			final JsonPathPassingClause passingClause = arguments.passingClause();
			if ( passingClause == null ) {
				arguments.jsonPath().accept( walker );
			}
			else {
				JsonPathHelper.appendJsonPathConcatPassingClause(
						sqlAppender,
						arguments.jsonPath(),
						passingClause, walker
				);
			}
			sqlAppender.appendSql( "),cast('null' as json))" );
			if ( decorationMode == DecorationMode.WRAP ) {
				sqlAppender.appendSql( ",']')" );
			}
			else if ( decorationMode == DecorationMode.TRIM ) {
				sqlAppender.appendSql( ")) as json)" );
			}
		}
	}

	enum DecorationMode { NONE, WRAP, TRIM }

	private static DecorationMode determineDecorationMode(
			JsonQueryArguments arguments,
			SqlAstTranslator<?> walker,
			JsonQueryWrapMode wrapMode) {
		if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
			final String jsonPath = walker.getLiteralValue( arguments.jsonPath() );
			if ( jsonPath.indexOf( '*' ) != -1 ) {
				// If the JSON path contains a star, MySQL will always wrap the result
				return DecorationMode.NONE;
			}
			else {
				// Otherwise we have to wrap the result manually
				return DecorationMode.WRAP;
			}
		}
		else if ( wrapMode == JsonQueryWrapMode.WITHOUT_WRAPPER ) {
			final String jsonPath = walker.getLiteralValue( arguments.jsonPath() );
			if ( jsonPath.indexOf( '*' ) != -1 ) {
				// If the JSON path contains a star, MySQL will always wrap the result,
				// so we have to trim the brackets
				return DecorationMode.TRIM;
			}
			else {
				// Nothing to do
				return DecorationMode.NONE;
			}
		}
		else {
			return DecorationMode.NONE;
		}
	}
}
