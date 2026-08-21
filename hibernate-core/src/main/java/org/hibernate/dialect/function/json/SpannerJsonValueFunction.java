/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;


import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.type.spi.TypeConfiguration;

public class SpannerJsonValueFunction extends JsonValueFunction {

	public SpannerJsonValueFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonValueArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final CastTarget returningType = arguments.returningType();
		if ( returningType != null ) {
			sqlAppender.appendSql( "cast(" );
		}
		sqlAppender.appendSql( "json_value(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ',' );
		// Spanner requires literal JSONPath expressions; named parameters (e.g. $idx) are not parsed and must be manually inlined.
		JsonPathHelper.appendInlinedJsonPathIncludingPassingClause(
				sqlAppender,
				"",
				arguments.jsonPath(),
				arguments.passingClause(),
				walker
		);
		sqlAppender.appendSql( ')' );
		if ( returningType != null ) {
			sqlAppender.appendSql( " as " );
			returningType.accept( walker );
			sqlAppender.appendSql( ')' );
		}
	}
}
