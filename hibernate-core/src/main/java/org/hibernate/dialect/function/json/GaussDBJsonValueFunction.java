/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.type.descriptor.jdbc.JsonHelper;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * GaussDB json_value function.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLJsonValueFunction.
 */
public class GaussDBJsonValueFunction extends JsonValueFunction {


	public GaussDBJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, true );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		if (arguments.returningType() != null) {
			sqlAppender.appendSql( "(" );
		}
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( "::json #>> '{" );
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
		if (arguments.returningType() != null) {
			sqlAppender.appendSql( ")::" );
			arguments.returningType().accept( walker );
		}
	}

	@Override
	protected void renderReturningClause(SqlAppender sqlAppender, JsonValueArguments arguments, SqlAstTranslator<?> walker) {
	}
}
