/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.dialect.aggregate.internal.DB2AggregateSupport;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * DB2 json_object function.
 */
public class DB2JsonObjectFunction extends JsonObjectFunction {

	public DB2JsonObjectFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, false );
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
