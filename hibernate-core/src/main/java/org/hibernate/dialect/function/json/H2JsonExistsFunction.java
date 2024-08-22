/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import org.hibernate.QueryException;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * H2 json_exists function.
 */
public class H2JsonExistsFunction extends JsonExistsFunction {

	public H2JsonExistsFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, true );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonExistsArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Json dereference errors by default if the JSON is invalid
		if ( arguments.errorBehavior() != null && arguments.errorBehavior() != JsonExistsErrorBehavior.ERROR ) {
			throw new QueryException( "Can't emulate on error clause on H2" );
		}
		final String jsonPath;
		try {
			jsonPath = walker.getLiteralValue( arguments.jsonPath() );
		}
		catch (Exception ex) {
			throw new QueryException( "H2 json_value only support literal json paths, but got " + arguments.jsonPath() );
		}
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( " is not null and " );
		H2JsonValueFunction.renderJsonPath(
				sqlAppender,
				arguments.jsonDocument(),
				arguments.isJsonType(),
				walker,
				jsonPath,
				arguments.passingClause()
		);
		sqlAppender.appendSql( " is not null" );
	}
}
