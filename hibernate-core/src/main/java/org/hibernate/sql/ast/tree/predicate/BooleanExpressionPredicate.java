/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Christian Beikov
 */
public class BooleanExpressionPredicate extends AbstractPredicate {
	private final Expression expression;

	public BooleanExpressionPredicate(Expression expression) {
		super( expression.getExpressionType(), false );
		this.expression = expression;
	}

	public BooleanExpressionPredicate(Expression expression, boolean negated, JdbcMappingContainer expressionType) {
		super( expressionType, negated );
		this.expression = expression;
	}

	public Expression getExpression() {
		return expression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitBooleanExpressionPredicate( this );
	}
}
