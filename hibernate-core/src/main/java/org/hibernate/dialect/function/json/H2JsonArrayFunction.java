/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.dialect.aggregate.internal.H2AggregateSupport;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * H2 json_array function.
 */
public class H2JsonArrayFunction extends JsonArrayFunction {

	public H2JsonArrayFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		final JdbcMappingContainer expressionType = ( (Expression) value ).getExpressionType();
		H2AggregateSupport.appendJsonWriteExpression( sqlAppender, () -> value.accept( walker ), expressionType.getSingleJdbcMapping() );
	}
}
