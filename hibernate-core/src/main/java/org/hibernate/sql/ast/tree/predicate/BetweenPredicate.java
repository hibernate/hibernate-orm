/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class BetweenPredicate extends AbstractPredicate {
	private final Expression expression;
	private final Expression lowerBound;
	private final Expression upperBound;

	public BetweenPredicate(
			Expression expression,
			Expression lowerBound,
			Expression upperBound,
			boolean negated,
			JdbcMappingContainer expressionType) {
		super( expressionType, negated );
		this.expression = expression;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public Expression getExpression() {
		return expression;
	}

	public Expression getLowerBound() {
		return lowerBound;
	}

	public Expression getUpperBound() {
		return upperBound;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitBetweenPredicate( this );
	}
}
