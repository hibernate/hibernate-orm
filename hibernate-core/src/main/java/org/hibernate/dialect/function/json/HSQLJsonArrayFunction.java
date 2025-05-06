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
 * HSQLDB json_array function.
 */
public class HSQLJsonArrayFunction extends JsonArrayFunction {

	public HSQLJsonArrayFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
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
