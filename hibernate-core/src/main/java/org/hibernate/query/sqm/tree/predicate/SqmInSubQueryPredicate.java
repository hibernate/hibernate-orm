/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

import jakarta.persistence.criteria.Expression;

import java.util.Objects;

import static org.hibernate.query.sqm.internal.TypecheckUtil.assertComparable;

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

		assertComparable( testExpression, subQueryExpression, nodeBuilder );

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
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		testExpression.appendHqlString( hql, context );
		if ( isNegated() ) {
			hql.append( " not" );
		}
		hql.append( " in " );
		subQueryExpression.appendHqlString( hql, context );
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

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmInSubQueryPredicate<?> that
			&& this.isNegated() == that.isNegated()
			&& Objects.equals( this.testExpression, that.testExpression )
			&& Objects.equals( this.subQueryExpression, that.subQueryExpression );
	}

	@Override
	public int hashCode() {
		return Objects.hash( testExpression, subQueryExpression, isNegated() );
	}
}
