/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Standard json_objectagg function that uses no returning clause.
 */
public class H2JsonObjectAggFunction extends JsonObjectAggFunction {

	public H2JsonObjectAggFunction(TypeConfiguration typeConfiguration) {
		super( ":", true, typeConfiguration );
	}

	@Override
	protected void renderReturningClause(
			SqlAppender sqlAppender,
			JsonObjectAggArguments arguments,
			SqlAstTranslator<?> translator) {
		// No-op
	}
}
