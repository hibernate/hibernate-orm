/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * MySQL json_exists function.
 */
public class MySQLJsonExistsFunction extends JsonExistsFunction {

	public MySQLJsonExistsFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonExistsArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final JsonPathPassingClause passingClause = arguments.passingClause();
		sqlAppender.appendSql( "json_contains_path(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ",'one'," );
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
		sqlAppender.appendSql( ')' );
	}
}
