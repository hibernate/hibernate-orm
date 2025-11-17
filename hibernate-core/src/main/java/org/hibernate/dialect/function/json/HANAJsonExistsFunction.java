/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SAP HANA json_exists function.
 */
public class HANAJsonExistsFunction extends JsonExistsFunction {

	public HANAJsonExistsFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonExistsArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_query(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ',' );
		final Expression jsonPath = arguments.jsonPath();
		final JsonPathPassingClause passingClause = arguments.passingClause();
		if ( passingClause == null ) {
			jsonPath.accept( walker );
		}
		else {
			JsonPathHelper.appendInlinedJsonPathIncludingPassingClause(
					sqlAppender,
					"",
					arguments.jsonPath(),
					passingClause,
					walker
			);
		}

		final JsonExistsErrorBehavior errorBehavior = arguments.errorBehavior();
		if ( errorBehavior != null && errorBehavior != JsonExistsErrorBehavior.FALSE ) {
			if ( errorBehavior == JsonExistsErrorBehavior.TRUE ) {
				sqlAppender.appendSql( " empty object on error" );
			}
			else {
				sqlAppender.appendSql( " error on error" );
			}
		}
		sqlAppender.appendSql( ") is not null" );
	}
}
