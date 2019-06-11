/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * @author Steve Ebersole
 */
public class InSubQueryPredicate implements Predicate {
	private final Expression testExpression;
	private final QuerySpec subQuery;
	private final boolean negated;

	public InSubQueryPredicate(Expression testExpression, QuerySpec subQuery, boolean negated) {
		this.testExpression = testExpression;
		this.subQuery = subQuery;
		this.negated = negated;
	}

	public Expression getTestExpression() {
		return testExpression;
	}

	public QuerySpec getSubQuery() {
		return subQuery;
	}

	public boolean isNegated() {
		return negated;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitInSubQueryPredicate( this );
	}
}
