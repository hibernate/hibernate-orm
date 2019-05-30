/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

/**
 * @author Steve Ebersole
 */
public class SqmInSubQueryPredicate<T> extends AbstractNegatableSqmPredicate implements SqmInPredicate<T> {
	private final SqmExpression<T> testExpression;
	private final SqmSubQuery<T> subQueryExpression;

	public SqmInSubQueryPredicate(
			SqmExpression<T> testExpression,
			SqmSubQuery<T> subQueryExpression,
			NodeBuilder nodeBuilder) {
		this( testExpression, subQueryExpression, false, nodeBuilder );
	}

	@SuppressWarnings("WeakerAccess")
	public SqmInSubQueryPredicate(
			SqmExpression<T> testExpression,
			SqmSubQuery<T> subQueryExpression,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( negated,  nodeBuilder );
		this.testExpression = testExpression;
		this.subQueryExpression = subQueryExpression;

		final SqmExpressable<?> expressableType = QueryHelper.highestPrecedenceType2(
				testExpression.getNodeType(),
				subQueryExpression.getNodeType()
		);

		testExpression.applyInferableType( expressableType );
		subQueryExpression.applyInferableType( expressableType );
	}

	@Override
	public SqmExpression<T> getTestExpression() {
		return testExpression;
	}

	@Override
	public SqmExpression<T> getExpression() {
		return getTestExpression();
	}

	public SqmSubQuery<T> getSubQueryExpression() {
		return subQueryExpression;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitInSubQueryPredicate( this );
	}

	@Override
	public SqmInPredicate<T> value(Object value) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SqmInPredicate<T> value(Expression value) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SqmInPredicate<T> value(JpaExpression value) {
		throw new UnsupportedOperationException(  );
	}
}
