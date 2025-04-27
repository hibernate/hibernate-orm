/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.SqmRenderContext;

/**
 * @author Steve Ebersole
 */
public class SqmCaseSimple<T, R>
		extends AbstractSqmExpression<R>
		implements JpaSimpleCase<T, R> {
	private final SqmExpression<T> fixture;
	private final List<WhenFragment<? extends T, ? extends R>> whenFragments;
	private SqmExpression<? extends R> otherwise;

	public SqmCaseSimple(SqmExpression<T> fixture, NodeBuilder nodeBuilder) {
		this( fixture, null, 10, nodeBuilder );
	}

	public SqmCaseSimple(SqmExpression<T> fixture, int estimatedWhenSize, NodeBuilder nodeBuilder) {
		this( fixture, null, estimatedWhenSize, nodeBuilder );
	}

	public SqmCaseSimple(SqmExpression<T> fixture, SqmExpressible<R> inherentType, NodeBuilder nodeBuilder) {
		this( fixture, inherentType, 10, nodeBuilder );
	}

	private SqmCaseSimple(
			SqmExpression<T> fixture,
			SqmExpressible<R> inherentType,
			int estimatedWhenSize,
			NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		this.whenFragments = new ArrayList<>( estimatedWhenSize );
		this.fixture = fixture;
	}

	@Override
	public SqmCaseSimple<T, R> copy(SqmCopyContext context) {
		final SqmCaseSimple<T, R> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCaseSimple<T, R> caseSearched = context.registerCopy(
				this,
				new SqmCaseSimple<>( fixture.copy( context ), getNodeType(), whenFragments.size(), nodeBuilder() )
		);
		for ( WhenFragment<? extends T, ? extends R> whenFragment : whenFragments ) {
			caseSearched.whenFragments.add(
					new SqmCaseSimple.WhenFragment<>(
							whenFragment.checkValue.copy( context ),
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

	public SqmExpression<T> getFixture() {
		return fixture;
	}

	public List<WhenFragment<? extends T,? extends R>> getWhenFragments() {
		return whenFragments;
	}

	public SqmExpression<? extends R> getOtherwise() {
		return otherwise;
	}

	public void otherwise(SqmExpression<? extends R> otherwiseExpression) {
		this.otherwise = otherwiseExpression;

		applyInferableResultType( otherwiseExpression.getNodeType() );
	}

	public void when(SqmExpression<? extends T> test, SqmExpression<? extends R> result) {
		whenFragments.add( new WhenFragment<>( test, result ) );

		// TODO: currently does nothing, but it would be nice if it worked!
		test.applyInferableType( fixture.getNodeType() );

		applyInferableResultType( result.getNodeType() );
	}

	private void applyInferableResultType(SqmExpressible<?> type) {
		if ( type != null ) {
			final SqmExpressible<?> oldType = getExpressible();
			final SqmExpressible<?> newType = QueryHelper.highestPrecedenceType2( oldType, type );
			if ( newType != null && newType != oldType ) {
				internalApplyInferableType( newType );
			}
		}
	}

	@Override
	protected void internalApplyInferableType(SqmExpressible<?> newType) {
		super.internalApplyInferableType( newType );

		if ( otherwise != null ) {
			otherwise.applyInferableType( newType );
		}

		if ( whenFragments != null ) {
			whenFragments.forEach( whenFragment -> whenFragment.getResult().applyInferableType( newType ) );
		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSimpleCaseExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<simple-case>";
	}

	public static class WhenFragment<T,R> {
		private final SqmExpression<T> checkValue;
		private final SqmExpression<R> result;

		public WhenFragment(SqmExpression<T> checkValue, SqmExpression<R> result) {
			this.checkValue = checkValue;
			this.result = result;
		}

		public SqmExpression<T> getCheckValue() {
			return checkValue;
		}

		public SqmExpression<R> getResult() {
			return result;
		}
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "case " );
		fixture.appendHqlString( hql, context );
		for ( WhenFragment<? extends T, ? extends R> whenFragment : whenFragments ) {
			hql.append( " when " );
			whenFragment.checkValue.appendHqlString( hql, context );
			hql.append( " then " );
			whenFragment.result.appendHqlString( hql, context );
		}

		if ( otherwise != null ) {
			hql.append( " else " );
			otherwise.appendHqlString( hql, context );
		}
		hql.append( " end" );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmCaseSimple<?, ?> that
			&& Objects.equals( this.fixture, that.fixture )
			&& Objects.equals( this.whenFragments, that.whenFragments )
			&& Objects.equals( this.otherwise, that.otherwise );
	}

	@Override
	public int hashCode() {
		return Objects.hash( fixture, whenFragments, otherwise );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public JpaExpression<T> getExpression() {
		return getFixture();
	}

	@Override
	public JpaSimpleCase<T, R> when(T condition, R result) {
		when( nodeBuilder().value( condition ), nodeBuilder().value( result ) );
		return this;
	}

	@Override
	public JpaSimpleCase<T, R> when(T condition, Expression<? extends R> result) {
		when( nodeBuilder().value( condition, fixture ), (SqmExpression<? extends R>) result );
		return this;
	}

	@Override
	public JpaSimpleCase<T, R> when(Expression<? extends T> condition, R result) {
		when( condition, nodeBuilder().value( result ) );
		return this;
	}

	@Override
	public JpaSimpleCase<T, R> when(Expression<? extends T> condition, Expression<? extends R> result) {
		when( (SqmExpression<? extends T>) condition, (SqmExpression<? extends R>) result );
		return this;
	}

	@Override
	public JpaSimpleCase<T, R> otherwise(R result) {
		otherwise( nodeBuilder().value( result ) );
		return this;
	}

	@Override
	public JpaSimpleCase<T, R> otherwise(Expression<? extends R> result) {
		otherwise( (SqmExpression<? extends R>) result );
		return this;
	}

}
