/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.predicate;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;
import org.hibernate.query.sqm.tree.spi.select.SqmSubQuery;

import jakarta.persistence.criteria.Expression;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.query.sqm.internal.TypecheckUtil.assertComparable;

/**
 * @author Steve Ebersole
 */
public class SqmInSubQueryPredicate<T> extends AbstractNegatableSqmPredicate implements SqmInPredicate<T> {
	private final SqmExpression<T> testExpression;
	private final SqmSubQuery<T> subQueryExpression;

	public SqmInSubQueryPredicate(
			@Nonnull SqmExpression<T> testExpression,
			@Nonnull SqmSubQuery<T> subQueryExpression,
			@Nonnull NodeBuilder nodeBuilder) {
		this( testExpression, subQueryExpression, false, nodeBuilder );
	}

	public SqmInSubQueryPredicate(
			@Nonnull SqmExpression<T> testExpression,
			@Nonnull SqmSubQuery<T> subQueryExpression,
			boolean negated,
			@Nonnull NodeBuilder nodeBuilder) {
		super( negated,  nodeBuilder );
		this.testExpression = testExpression;
		this.subQueryExpression = subQueryExpression;

		assertComparable( testExpression, subQueryExpression, nodeBuilder );

		final SqmBindableType<?> expressibleType = QueryHelper.highestPrecedenceType2(
				testExpression.getExpressible(),
				subQueryExpression.getExpressible()
		);
		final JavaType<?> javaType = QueryHelper.highestPrecedenceType2(
				testExpression.getJavaTypeDescriptor(),
				subQueryExpression.getJavaTypeDescriptor()
		);

		testExpression.applyInferableType( expressibleType, javaType );
		subQueryExpression.applyInferableType( expressibleType, javaType );
	}

	@Nonnull
	@Override
	public SqmInSubQueryPredicate<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
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

	@Nonnull
	@Override
	public SqmExpression<T> getTestExpression() {
		return testExpression;
	}

	@Override
	@Nonnull
	public SqmExpression<T> getExpression() {
		return getTestExpression();
	}

	@Nonnull
	public SqmSubQuery<T> getSubQueryExpression() {
		return subQueryExpression;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitInSubQueryPredicate( this );
	}

	@Override
	@Nonnull
	public SqmInPredicate<T> value(@Nonnull T value) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@Nonnull
	public SqmInPredicate<T> value(@Nonnull Expression<? extends T> value) {
		throw new UnsupportedOperationException(  );
	}

	@Nonnull
	@Override
	public SqmInPredicate<T> value(@Nonnull JpaExpression<? extends T> value) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		testExpression.appendHqlString( hql, context );
		if ( isNegated() ) {
			hql.append( " not" );
		}
		hql.append( " in " );
		subQueryExpression.appendHqlString( hql, context );
	}

	@Nonnull
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
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmInSubQueryPredicate<?> that
			&& this.isNegated() == that.isNegated()
			&& this.testExpression.equals( that.testExpression )
			&& this.subQueryExpression.equals( that.subQueryExpression );
	}

	@Override
	public int hashCode() {
		int result = testExpression.hashCode();
		result = 31 * result + subQueryExpression.hashCode();
		result = 31 * result + Boolean.hashCode( isNegated() );
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmInSubQueryPredicate<?> that
			&& this.isNegated() == that.isNegated()
			&& this.testExpression.isCompatible( that.testExpression )
			&& this.subQueryExpression.isCompatible( that.subQueryExpression );
	}

	@Override
	public int cacheHashCode() {
		int result = testExpression.cacheHashCode();
		result = 31 * result + subQueryExpression.cacheHashCode();
		result = 31 * result + Boolean.hashCode( isNegated() );
		return result;
	}
}
