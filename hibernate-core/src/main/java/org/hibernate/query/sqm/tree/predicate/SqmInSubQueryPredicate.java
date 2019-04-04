/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmSubQuery;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * @author Steve Ebersole
 */
public class SqmInSubQueryPredicate extends AbstractNegatableSqmPredicate implements SqmInPredicate {
	private final SqmExpression testExpression;
	private final SqmSubQuery subQueryExpression;

	public SqmInSubQueryPredicate(
			SqmExpression testExpression,
			SqmSubQuery subQueryExpression) {
		this( testExpression, subQueryExpression, false );
	}

	public SqmInSubQueryPredicate(
			SqmExpression testExpression,
			SqmSubQuery subQueryExpression,
			boolean negated) {
		super( negated );
		this.testExpression = testExpression;
		this.subQueryExpression = subQueryExpression;

		final ExpressableType<?> expressableType = QueryHelper.highestPrecedenceType(
				testExpression.getExpressableType(),
				subQueryExpression.getExpressableType()
		);

		testExpression.applyInferableType( expressableType );
		subQueryExpression.applyInferableType( expressableType );
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
