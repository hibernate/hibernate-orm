/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.dialect.aggregate.H2AggregateSupport;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * H2 json_object function.
 */
public class H2JsonObjectFunction extends JsonObjectFunction {

	public H2JsonObjectFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true );
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		final JdbcMappingContainer expressionType = ( (Expression) value ).getExpressionType();
		H2AggregateSupport.appendJsonWriteExpression( sqlAppender, () -> value.accept( walker ), expressionType.getSingleJdbcMapping() );
	}
}
