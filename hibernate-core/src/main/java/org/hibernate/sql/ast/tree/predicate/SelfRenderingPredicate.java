/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;

/**
 *
 * @author Chris Cranford
 */
public class SelfRenderingPredicate implements Predicate {
	private final SelfRenderingExpression selfRenderingExpression;

	public SelfRenderingPredicate(SelfRenderingExpression expression) {
		this.selfRenderingExpression = expression;
	}

	public SelfRenderingExpression getSelfRenderingExpression() {
		return selfRenderingExpression;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSelfRenderingPredicate( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return selfRenderingExpression.getExpressionType();
	}
}
