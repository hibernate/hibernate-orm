/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.dialect.JsonHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Iterator;
import java.util.Map;

/**
 * PostgreSQL json_query function.
 */
public class GaussdbJsonQueryFunction extends JsonQueryFunction {

	public GaussdbJsonQueryFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, true );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonQueryArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		if ( arguments.wrapMode() == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( "json_build_array(" );
		}
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( "::json #> '{" );
		String literalValue = walker.getLiteralValue( arguments.jsonPath() );

		final JsonPathPassingClause passingClause = arguments.passingClause();
		if ( passingClause != null ) {
			final Map<String, Expression> passingExpressions = passingClause.getPassingExpressions();
			final Iterator<Map.Entry<String, Expression>> iterator = passingExpressions.entrySet().iterator();
			Map.Entry<String, Expression> entry = iterator.next();
			literalValue = literalValue.replace( "$"+entry.getKey(), walker.getLiteralValue( entry.getValue()).toString() );
			while ( iterator.hasNext() ) {
				entry = iterator.next();
				sqlAppender.appendSql( ',' );
				literalValue = literalValue.replace( "$"+entry.getKey(), walker.getLiteralValue( entry.getValue()).toString() );
			}
		}

		sqlAppender.append( JsonHelper.parseJsonPath( literalValue ) );
		sqlAppender.appendSql( "}'" );
		if ( arguments.wrapMode() == JsonQueryWrapMode.WITH_WRAPPER ) {
			sqlAppender.appendSql( ")" );
		}
	}

}
