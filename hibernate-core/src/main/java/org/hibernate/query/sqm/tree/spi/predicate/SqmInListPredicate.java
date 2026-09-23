/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.predicate;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.Nonnull;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;

import jakarta.persistence.criteria.Expression;

import static org.hibernate.query.internal.QueryHelper.highestPrecedenceType2;
import static org.hibernate.query.sqm.internal.TypecheckUtil.assertComparable;

/**
 * @author Steve Ebersole
 */
public class SqmInListPredicate<T> extends AbstractNegatableSqmPredicate implements SqmInPredicate<T> {
	private final SqmExpression<T> testExpression;
	private final List<SqmExpression<T>> listExpressions;

	public SqmInListPredicate(@Nonnull SqmExpression<T> testExpression, @Nonnull NodeBuilder nodeBuilder) {
		this( testExpression, new ArrayList<>(), nodeBuilder );
	}

	@SuppressWarnings({"unchecked", "unused"})
	public SqmInListPredicate(
			@Nonnull SqmExpression<T> testExpression,
			@Nonnull NodeBuilder nodeBuilder,
			@Nonnull SqmExpression<T>... listExpressions) {
		this( testExpression, ArrayHelper.toExpandableList( listExpressions ), nodeBuilder );
	}

	public SqmInListPredicate(
			@Nonnull SqmExpression<T> testExpression,
			@Nonnull List<? extends SqmExpression<T>> listExpressions,
			@Nonnull NodeBuilder nodeBuilder) {
		this( testExpression, listExpressions, false, nodeBuilder );
	}

	public SqmInListPredicate(
			@Nonnull SqmExpression<T> testExpression,
			@Nonnull List<? extends SqmExpression<T>> listExpressions,
			boolean negated,
			@Nonnull NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.testExpression = testExpression;
		//noinspection unchecked
		this.listExpressions = (List<SqmExpression<T>>) listExpressions;
		for ( SqmExpression<T> listExpression : listExpressions ) {
			implyListElementType( listExpression, testExpression, nodeBuilder );
		}
	}

	@Nonnull
	@Override
	public SqmInListPredicate<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		List<SqmExpression<T>> listExpressions = new ArrayList<>( this.listExpressions.size() );
		for ( SqmExpression<T> listExpression : this.listExpressions ) {
			listExpressions.add( listExpression.copy( context ) );
		}
		final SqmInListPredicate<T> predicate = context.registerCopy(
				this,
				new SqmInListPredicate<>(
						testExpression.copy( context ),
						listExpressions,
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

	@Override
	@Nonnull
	public SqmInPredicate<T> value(@Nonnull T value) {
		if ( value instanceof Collection ) {
			//noinspection unchecked
			for ( T v : ( (Collection<T>) value ) ) {
				addExpression( nodeBuilder().value( v, testExpression ) );
			}
		}
		else {
			addExpression( nodeBuilder().value( (T) value, testExpression ) );
		}

		return this;
	}

	@Override
	@Nonnull
	public SqmInPredicate<T> value(@Nonnull Expression<? extends T> value) {
		//noinspection unchecked
		addExpression( (SqmExpression<T>) value );
		return this;
	}

	@Nonnull
	@Override
	public SqmInPredicate<T> value(@Nonnull JpaExpression<? extends T> value) {
		//noinspection unchecked
		addExpression( (SqmExpression<T>) value );
		return this;
	}

	@Nonnull
	public List<? extends SqmExpression<T>> getListExpressions() {
		return listExpressions;
	}

	public void addExpression(@Nonnull SqmExpression<T> expression) {
		implyListElementType( expression );

		listExpressions.add( expression );
	}

	private void implyListElementType(@Nonnull SqmExpression<?> expression) {
		implyListElementType( expression, getTestExpression(), nodeBuilder() );
	}

	private static void implyListElementType(@Nonnull SqmExpression<?> expression, @Nonnull SqmExpression<?> testExpression, @Nonnull NodeBuilder nodeBuilder) {
		assertComparable( testExpression, expression, nodeBuilder );
		expression.applyInferableType(
				highestPrecedenceType2( testExpression.getExpressible(), expression.getExpressible() ),
				highestPrecedenceType2( testExpression.getJavaTypeDescriptor(), expression.getJavaTypeDescriptor() )
		);
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitInListPredicate( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		testExpression.appendHqlString( hql, context );
		if ( isNegated() ) {
			hql.append( " not" );
		}
		hql.append( " in (" );
		listExpressions.get( 0 ).appendHqlString( hql, context );
		for ( int i = 1; i < listExpressions.size(); i++ ) {
			hql.append( ", " );
			listExpressions.get( i ).appendHqlString( hql, context );
		}
		hql.append( ')' );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmInListPredicate<?> that
			&& this.isNegated() == that.isNegated()
			&& this.testExpression.equals( that.testExpression )
			&& Objects.equals( this.listExpressions, that.listExpressions );
	}

	@Override
	public int hashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + testExpression.hashCode();
		result = 31 * result + Objects.hashCode( listExpressions );
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmInListPredicate<?> that
			&& this.isNegated() == that.isNegated()
			&& this.testExpression.isCompatible( that.testExpression )
			&& SqmCacheable.areCompatible( this.listExpressions, that.listExpressions );
	}

	@Override
	public int cacheHashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + testExpression.cacheHashCode();
		result = 31 * result + SqmCacheable.cacheHashCode( listExpressions );
		return result;
	}

	@Nonnull
	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmInListPredicate<>( testExpression, listExpressions, !isNegated(), nodeBuilder() );
	}
}
