/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * H2 json_arrayagg function.
 */
public class H2JsonArrayAggFunction extends JsonArrayAggFunction {

	public H2JsonArrayAggFunction(TypeConfiguration typeConfiguration) {
		super( true, typeConfiguration );
	}

	@Override
	protected void renderReturningClause(SqlAppender sqlAppender, Expression arg, SqlAstTranslator<?> translator) {
		// No returning clause supported or needed
	}
}
