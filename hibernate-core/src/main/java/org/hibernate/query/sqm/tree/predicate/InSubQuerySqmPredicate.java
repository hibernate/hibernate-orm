/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmSubQuery;

/**
 * @author Steve Ebersole
 */
public class InSubQuerySqmPredicate extends AbstractNegatableSqmPredicate implements InSqmPredicate {
	private final SqmExpression testExpression;
	private final SqmSubQuery subQueryExpression;

	public InSubQuerySqmPredicate(
			SqmExpression testExpression,
			SqmSubQuery subQueryExpression) {
		this( testExpression, subQueryExpression, false );
	}

	public InSubQuerySqmPredicate(
			SqmExpression testExpression,
			SqmSubQuery subQueryExpression,
			boolean negated) {
		super( negated );
		this.testExpression = testExpression;
		this.subQueryExpression = subQueryExpression;
	}

	@Override
	public SqmExpression getTestExpression() {
		return testExpression;
	}

	public SqmSubQuery getSubQueryExpression() {
		return subQueryExpression;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitInSubQueryPredicate( this );
	}
}
