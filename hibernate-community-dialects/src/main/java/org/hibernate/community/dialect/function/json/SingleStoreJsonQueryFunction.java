/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.json.JsonPathHelper;
import org.hibernate.dialect.function.json.JsonQueryFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SingleStore json_query function.
 */
public class SingleStoreJsonQueryFunction extends JsonQueryFunction {

	public SingleStoreJsonQueryFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonQueryArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonQueryErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on SingleStore" );
		}
		if ( arguments.emptyBehavior() != null && arguments.emptyBehavior() != JsonQueryEmptyBehavior.NULL ) {
			throw new QueryException( "Can't emulate on empty clause on SingleStore" );
		}
		else {
			final String jsonPath;
			try {
				jsonPath = walker.getLiteralValue( arguments.jsonPath() );
			}
			catch (Exception ex) {
				throw new QueryException( "SingleStore json_query only support literal json paths, but got " + arguments.jsonPath() );
			}
			final List<JsonPathHelper.JsonPathElement> jsonPathElements = JsonPathHelper.parseJsonPathElements( jsonPath );
			final JsonQueryWrapMode wrapMode = arguments.wrapMode();
			final DecorationMode decorationMode = determineDecorationMode( wrapMode );
			if ( decorationMode == DecorationMode.WRAP ) {
				sqlAppender.appendSql( "concat('['," );
			}
			sqlAppender.appendSql( "nullif(json_extract_string(" );
			arguments.jsonDocument().accept( walker );
			for ( JsonPathHelper.JsonPathElement pathElement : jsonPathElements ) {
				sqlAppender.appendSql( ',' );
				if ( pathElement instanceof JsonPathHelper.JsonAttribute attribute ) {
					sqlAppender.appendSingleQuoteEscapedString( attribute.attribute() );
				}
				else if ( pathElement instanceof JsonPathHelper.JsonParameterIndexAccess indexParameter) {
					final String parameterName = indexParameter.parameterName();
					throw new QueryException( "JSON path [" + jsonPath + "] uses parameter [" + parameterName + "] that is not passed" );
				}
				else {
					sqlAppender.appendSql( '\'' );
					sqlAppender.appendSql( ( (JsonPathHelper.JsonIndexAccess) pathElement ).index() );
					sqlAppender.appendSql( '\'' );
				}
			}
			sqlAppender.appendSql( "),'null')" );
			if ( decorationMode == DecorationMode.WRAP ) {
				sqlAppender.appendSql( ",']')" );
			}
		}
	}

	enum DecorationMode {NONE, WRAP}

	private static DecorationMode determineDecorationMode(JsonQueryWrapMode wrapMode) {
		if ( wrapMode == JsonQueryWrapMode.WITH_WRAPPER ) {
			return DecorationMode.WRAP;
		}
		return DecorationMode.NONE;
	}
}
