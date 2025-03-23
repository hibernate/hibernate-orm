/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;

/**
 * Represents a self rendering expression that renders a SQL fragment.
 *
 * @author Christian Beikov
 */
public class SelfRenderingSqlFragmentExpression implements SelfRenderingExpression {
	private final String expression;
	private final @Nullable JdbcMappingContainer expressionType;

	public SelfRenderingSqlFragmentExpression(String expression) {
		this( expression, null );
	}

	public SelfRenderingSqlFragmentExpression(String expression, @Nullable JdbcMappingContainer expressionType) {
		this.expression = expression;
		this.expressionType = expressionType;
	}

	public String getExpression() {
		return expression;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return expressionType;
	}

	@Override
	public void renderToSql(
			SqlAppender sqlAppender,
			SqlAstTranslator<?> walker,
			SessionFactoryImplementor sessionFactory) {
		sqlAppender.append( expression );
	}
}
