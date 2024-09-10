/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Oracle json_arrayagg function.
 */
public class OracleJsonArrayAggFunction extends JsonArrayAggFunction {

	public OracleJsonArrayAggFunction(TypeConfiguration typeConfiguration) {
		super( false, typeConfiguration );
	}

	@Override
	protected void renderArgument(
			SqlAppender sqlAppender,
			Expression arg,
			JsonNullBehavior nullBehavior,
			SqlAstTranslator<?> translator) {
		arg.accept( translator );
		final JdbcMappingContainer expressionType = arg.getExpressionType();
		if ( expressionType != null && expressionType.getSingleJdbcMapping().getJdbcType().isJson()
				&& !SqlTypes.isJsonType( expressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
			sqlAppender.appendSql( " format json" );
		}
	}
}
