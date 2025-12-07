/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public class SqmCaseSearched<R>
		extends AbstractSqmExpression<R>
		implements JpaSearchedCase<R> {
	private final List<WhenFragment<? extends R>> whenFragments;
	private @Nullable SqmExpression<? extends R> otherwise;

	public SqmCaseSearched(NodeBuilder nodeBuilder) {
		this( null, nodeBuilder );
	}

	public SqmCaseSearched(@Nullable SqmBindableType<R> inherentType, NodeBuilder nodeBuilder) {
		this( inherentType, 10, nodeBuilder );
	}

	public SqmCaseSearched(int estimatedWhenSize, NodeBuilder nodeBuilder) {
		this( null, estimatedWhenSize, nodeBuilder );
	}

	private SqmCaseSearched(@Nullable SqmBindableType<R> inherentType, int estimatedWhenSize, NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		this.whenFragments = new ArrayList<>( estimatedWhenSize );
	}

	@Override
	public SqmCaseSearched<R> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCaseSearched<R> caseSearched = context.registerCopy(
				this,
				new SqmCaseSearched<>( getNodeType(), whenFragments.size(), nodeBuilder() )
		);
		for ( WhenFragment<? extends R> whenFragment : whenFragments ) {
			caseSearched.whenFragments.add(
					new WhenFragment<>(
							whenFragment.predicate.copy( context ),
							whenFragment.result.copy( context )
					)
			);
		}
		if ( otherwise != null ) {
			caseSearched.otherwise = otherwise.copy( context );
		}
		copyTo( caseSearched, context );
		return caseSearched;
	}

	public List<WhenFragment<? extends R>> getWhenFragments() {
		return whenFragments;
	}

	public @Nullable SqmExpression<? extends R> getOtherwise() {
		return otherwise;
	}

	public SqmCaseSearched<R> when(SqmPredicate predicate, SqmExpression<? extends R> result) {
		whenFragments.add( new WhenFragment<>( predicate, result ) );
		applyInferableResultType( result.getNodeType() );
		return this;
	}

	public SqmCaseSearched<R> otherwise(SqmExpression<? extends R> otherwiseExpression) {
		this.otherwise = otherwiseExpression;
		applyInferableResultType( otherwiseExpression.getNodeType() );
		return this;
	}

	private void applyInferableResultType(@Nullable SqmBindableType<?> type) {
		if ( type != null ) {
			final SqmBindableType<?> oldType = getExpressible();
			final SqmBindableType<?> newType = QueryHelper.highestPrecedenceType2( oldType, type );
			if ( newType != null && newType != oldType ) {
				internalApplyInferableType( newType );
			}
		}
	}

	@Override
	protected void internalApplyInferableType(@Nullable SqmBindableType<?> newType) {
		super.internalApplyInferableType( newType );

		if ( otherwise != null ) {
			otherwise.applyInferableType( newType );
		}

		if ( whenFragments != null ) {
			whenFragments.forEach( whenFragment -> whenFragment.getResult().applyInferableType( newType ) );
		}
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitSearchedCaseExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<searched-case>";
	}

	public static class WhenFragment<R> implements SqmCacheable {
		private final SqmPredicate predicate;
		private final SqmExpression<R> result;

		public WhenFragment(SqmPredicate predicate, SqmExpression<R> result) {
			this.predicate = predicate;
			this.result = result;
		}

		public SqmPredicate getPredicate() {
			return predicate;
		}

		public SqmExpression<R> getResult() {
			return result;
		}

		@Override
		public boolean equals(@Nullable Object object) {
			return object instanceof WhenFragment<?> that
				&& predicate.equals( that.predicate )
				&& result.equals( that.result );
		}

		@Override
		public int hashCode() {
			int result = predicate.hashCode();
			result = 31 * result + this.result.hashCode();
			return result;
		}

		@Override
		public boolean isCompatible(Object object) {
			return object instanceof WhenFragment<?> that
					&& predicate.isCompatible( that.predicate )
					&& result.isCompatible( that.result );
		}

		@Override
		public int cacheHashCode() {
			int result = predicate.cacheHashCode();
			result = 31 * result + this.result.cacheHashCode();
			return result;
		}
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "case" );
		for ( WhenFragment<? extends R> whenFragment : whenFragments ) {
			hql.append( " when " );
			whenFragment.predicate.appendHqlString( hql, context );
			hql.append( " then " );
			whenFragment.result.appendHqlString( hql, context );
		}

		final SqmExpression<? extends R> otherwise = this.otherwise;
		if ( otherwise != null ) {
			hql.append( " else " );
			otherwise.appendHqlString( hql, context );
		}
		hql.append( " end" );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmCaseSearched<?> that
			&& Objects.equals( this.whenFragments, that.whenFragments )
			&& Objects.equals( this.otherwise, that.otherwise );
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode( whenFragments );
		result = 31 * result + Objects.hashCode( otherwise );
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmCaseSearched<?> that
			&& SqmCacheable.areCompatible( this.whenFragments, that.whenFragments )
			&& SqmCacheable.areCompatible( this.otherwise, that.otherwise );
	}

	@Override
	public int cacheHashCode() {
		int result = SqmCacheable.cacheHashCode( whenFragments );
		result = 31 * result + SqmCacheable.cacheHashCode( otherwise );
		return result;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmCaseSearched<R> when(Expression<Boolean> condition, @Nullable R result) {
		when( nodeBuilder().wrap( condition ), nodeBuilder().value( result, otherwise ) );
		return this;
	}

	@Override
	public SqmCaseSearched<R> when(Expression<Boolean> condition, Expression<? extends R> result) {
		when( nodeBuilder().wrap( condition ), (SqmExpression<? extends R>) result );
		return this;
	}

	@Override
	public SqmExpression<R> otherwise(@Nullable R result) {
		otherwise( nodeBuilder().value( result ) );
		return this;
	}

	@Override
	public SqmExpression<R> otherwise(Expression<? extends R> result) {
		otherwise( (SqmExpression<? extends R>) result );
		return this;
	}

}
