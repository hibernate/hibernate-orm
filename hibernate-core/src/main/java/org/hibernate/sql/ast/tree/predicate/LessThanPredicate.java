/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author chen zhida
 */
public class LessThanPredicate extends AbstractPredicate {

	private final Expression expression;
	private final Expression upperBound;

	public LessThanPredicate(
			Expression expression,
			Expression upperBound,
			boolean negated,
			JdbcMappingContainer expressionType) {
		super( expressionType, negated );
		this.expression = expression;
		this.upperBound = upperBound;
	}

	public Expression getExpression() {
		return expression;
	}

	public Expression getUpperBound() {
		return upperBound;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitLessThanPredicate( this );
	}
}
