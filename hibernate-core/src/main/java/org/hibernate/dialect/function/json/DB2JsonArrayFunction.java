/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.dialect.aggregate.DB2AggregateSupport;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * DB2 json_array function.
 */
public class DB2JsonArrayFunction extends JsonArrayFunction {

	public DB2JsonArrayFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		if ( value instanceof Expression expression && expression.getExpressionType() != null ) {
			final JdbcMapping jdbcMapping = expression.getExpressionType().getSingleJdbcMapping();
			DB2AggregateSupport.appendJsonWriteExpression( sqlAppender, () -> value.accept( walker ), jdbcMapping );
		}
		else {
			value.accept( walker );
			if ( ExpressionTypeHelper.isJson( value ) ) {
				sqlAppender.appendSql( " format json" );
			}
		}
	}
}
