/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * MariaDB json_objectagg function.
 */
public class MariaDBJsonObjectAggFunction extends MySQLJsonObjectAggFunction {

	public MariaDBJsonObjectAggFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	protected void renderArgument(
			SqlAppender sqlAppender,
			Expression arg,
			JsonNullBehavior nullBehavior,
			SqlAstTranslator<?> translator) {
		// Convert SQL type to JSON type
		if ( nullBehavior != JsonNullBehavior.NULL ) {
			sqlAppender.appendSql( "nullif(" );
		}
		sqlAppender.appendSql( "json_extract(json_array(" );
		arg.accept( translator );
		sqlAppender.appendSql( "),'$[0]')" );
		if ( nullBehavior != JsonNullBehavior.NULL ) {
			sqlAppender.appendSql( ",'null')" );
		}
	}
}
