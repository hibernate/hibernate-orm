/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * HSQLDB json_object function.
 */
public class HSQLJsonObjectFunction extends JsonObjectFunction {

	public HSQLJsonObjectFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true );
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		if ( value instanceof Literal literal && literal.getLiteralValue() == null ) {
			sqlAppender.appendSql( "cast(null as int)" );
		}
		else {
			value.accept( walker );
		}
	}
}
