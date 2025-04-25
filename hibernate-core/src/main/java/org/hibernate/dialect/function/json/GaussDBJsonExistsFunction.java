/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Iterator;
import java.util.Map;


/**
 * PostgreSQL json_query function.
 */
public class GaussDBJsonExistsFunction extends JsonExistsFunction {

	public GaussDBJsonExistsFunction(TypeConfiguration typeConfiguration,
									 boolean supportsJsonPathExpression,
									 boolean supportsJsonPathPassingClause) {
		super(typeConfiguration, supportsJsonPathExpression, supportsJsonPathPassingClause);
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonExistsArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		sqlAppender.appendSql( "json_contains_path(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ",'one', '" );

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

		sqlAppender.appendSql( literalValue );
		sqlAppender.appendSql( "') = 1" );
	}

}
