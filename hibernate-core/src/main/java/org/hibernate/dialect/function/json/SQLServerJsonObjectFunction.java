/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server json_object function.
 */
public class SQLServerJsonObjectFunction extends JsonObjectFunction {

	public SQLServerJsonObjectFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true );
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		if ( ExpressionTypeHelper.isBoolean( value ) ) {
			sqlAppender.appendSql( "cast(" );
			value.accept( walker );
			sqlAppender.appendSql( " as bit)" );
		}
		else {
			value.accept( walker );
		}
	}
}
