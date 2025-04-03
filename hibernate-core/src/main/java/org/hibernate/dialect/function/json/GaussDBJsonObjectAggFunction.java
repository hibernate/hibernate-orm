/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * GaussDB json_objectagg function.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLJsonObjectAggFunction.
 */
public class GaussDBJsonObjectAggFunction extends JsonObjectAggFunction {


	public GaussDBJsonObjectAggFunction(boolean supportsStandard, TypeConfiguration typeConfiguration) {
		super( ":", true, typeConfiguration );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonObjectAggArguments arguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {

		sqlAppender.appendSql( "json_object_agg(" );
		sqlAppender.appendSql( "CASE WHEN " );
		arguments.key().accept( translator );
		sqlAppender.appendSql( " IS NOT NULL  " );
		sqlAppender.appendSql( " THEN " );
		arguments.key().accept( translator );
		sqlAppender.appendSql( " END," );
		arguments.value().accept( translator );
		sqlAppender.appendSql( ")" );
	}

	@Override
	protected void renderUniqueAndReturningClause(SqlAppender sqlAppender, JsonObjectAggArguments arguments, SqlAstTranslator<?> translator) {
		renderUniqueClause( sqlAppender, arguments, translator );
		renderReturningClause( sqlAppender, arguments, translator );
	}
}
