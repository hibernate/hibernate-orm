/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

import jakarta.persistence.criteria.Expression;

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

	public SqmInSubQueryPredicate(
			SqmExpression<T> testExpression,
			SqmSubQuery<T> subQueryExpression,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( negated,  nodeBuilder );
		this.testExpression = testExpression;
		this.subQueryExpression = subQueryExpression;

		final SqmExpressible<?> expressibleType = QueryHelper.highestPrecedenceType2(
				testExpression.getExpressible(),
				subQueryExpression.getExpressible()
		);

		testExpression.applyInferableType( expressibleType );
		subQueryExpression.applyInferableType( expressibleType );
	}

	@Override
	public SqmInSubQueryPredicate<T> copy(SqmCopyContext context) {
		final SqmInSubQueryPredicate<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmInSubQueryPredicate<T> predicate = context.registerCopy(
				this,
				new SqmInSubQueryPredicate<T>(
						testExpression.copy( context ),
						subQueryExpression.copy( context ),
						isNegated(),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
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
	public SqmInPredicate<T> value(JpaExpression<? extends T> value) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		testExpression.appendHqlString( sb );
		if ( isNegated() ) {
			sb.append( " not" );
		}
		sb.append( " in " );
		subQueryExpression.appendHqlString( sb );
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmInSubQueryPredicate<>(
				testExpression,
				subQueryExpression,
				!isNegated(),
				nodeBuilder()
		);
	}
}
