/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Gavin King
 */
public class Distinct implements Expression, SqlExpressible, SqlAstNode {
	private final Expression expression;

	public Distinct(Expression expression) {
		this.expression = expression;
	}

	public Expression getExpression() {
		return expression;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		if ( expression instanceof SqlExpressible sqlExpressible) {
			return sqlExpressible.getJdbcMapping();
		}

		if ( getExpressionType() instanceof SqlExpressible sqlExpressible ) {
			return sqlExpressible.getJdbcMapping();
		}

		if ( getExpressionType() != null ) {
			assert getExpressionType().getJdbcTypeCount() == 1;
			return getExpressionType().getSingleJdbcMapping();
		}

		return null;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return expression.getExpressionType();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitDistinct( this );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
