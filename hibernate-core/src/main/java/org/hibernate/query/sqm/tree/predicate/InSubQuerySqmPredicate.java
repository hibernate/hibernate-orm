/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SubQuerySqmExpression;

/**
 * @author Steve Ebersole
 */
public class InSubQuerySqmPredicate extends AbstractNegatableSqmPredicate implements InSqmPredicate {
	private final SqmExpression testExpression;
	private final SubQuerySqmExpression subQueryExpression;

	public InSubQuerySqmPredicate(
			SqmExpression testExpression,
			SubQuerySqmExpression subQueryExpression) {
		this( testExpression, subQueryExpression, false );
	}

	public InSubQuerySqmPredicate(
			SqmExpression testExpression,
			SubQuerySqmExpression subQueryExpression,
			boolean negated) {
		super( negated );
		this.testExpression = testExpression;
		this.subQueryExpression = subQueryExpression;
	}

	@Override
	public SqmExpression getTestExpression() {
		return testExpression;
	}

	public SubQuerySqmExpression getSubQueryExpression() {
		return subQueryExpression;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitInSubQueryPredicate( this );
	}
}
