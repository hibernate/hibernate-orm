/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
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
		}
	}
}
