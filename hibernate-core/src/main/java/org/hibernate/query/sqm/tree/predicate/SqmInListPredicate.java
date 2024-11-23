/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import jakarta.persistence.criteria.Expression;

import static org.hibernate.query.sqm.internal.TypecheckUtil.assertComparable;

/**
 * @author Steve Ebersole
 */
public class SqmInListPredicate<T> extends AbstractNegatableSqmPredicate implements SqmInPredicate<T> {
	private final SqmExpression<T> testExpression;
	private final List<SqmExpression<T>> listExpressions;

	public SqmInListPredicate(SqmExpression<T> testExpression, NodeBuilder nodeBuilder) {
		this( testExpression, new ArrayList<>(), nodeBuilder );
	}

	@SuppressWarnings({"unchecked", "unused"})
	public SqmInListPredicate(
			SqmExpression<T> testExpression,
			NodeBuilder nodeBuilder,
			SqmExpression<T>... listExpressions) {
		this( testExpression, ArrayHelper.toExpandableList( listExpressions ), nodeBuilder );
	}

	public SqmInListPredicate(
			SqmExpression<T> testExpression,
			List<? extends SqmExpression<T>> listExpressions,
			NodeBuilder nodeBuilder) {
		this( testExpression, listExpressions, false, nodeBuilder );
	}

	public SqmInListPredicate(
			SqmExpression<T> testExpression,
			List<? extends SqmExpression<T>> listExpressions,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.testExpression = testExpression;
		//noinspection unchecked
		this.listExpressions = (List<SqmExpression<T>>) listExpressions;
		for ( SqmExpression<T> listExpression : listExpressions ) {
			implyListElementType( listExpression );
		}
	}

	@Override
	public SqmInListPredicate<T> copy(SqmCopyContext context) {
		final SqmInListPredicate<T> existing = context.getCopy( this );
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

	@Override
	public SqmExpression<T> getTestExpression() {
		return testExpression;
	}

	@Override
	public SqmExpression<T> getExpression() {
		return getTestExpression();
	}

	@Override
	public SqmInPredicate<T> value(Object value) {
		if ( value instanceof Collection ) {
			//noinspection unchecked
			for ( T v : ( (Collection<T>) value ) ) {
				addExpression( nodeBuilder().value( v, testExpression ) );
			}
		}
		else {
			//noinspection unchecked
			addExpression( nodeBuilder().value( (T) value, testExpression ) );
		}

		return this;
	}

	@Override
	public SqmInPredicate<T> value(Expression<? extends T> value) {
		//noinspection unchecked
		addExpression( (SqmExpression<T>) value );
		return this;
	}

	@Override
	public SqmInPredicate<T> value(JpaExpression<? extends T> value) {
		//noinspection unchecked
		addExpression( (SqmExpression<T>) value );
		return this;
	}

	public List<? extends SqmExpression<T>> getListExpressions() {
		return listExpressions;
	}

	public void addExpression(SqmExpression<T> expression) {
		implyListElementType( expression );

		listExpressions.add( expression );
	}

	private void implyListElementType(SqmExpression<?> expression) {
		assertComparable( getTestExpression(), expression, nodeBuilder() );
		expression.applyInferableType(
				QueryHelper.highestPrecedenceType2( getTestExpression().getExpressible(), expression.getExpressible() )
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitInListPredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		testExpression.appendHqlString( sb );
		if ( isNegated() ) {
			sb.append( " not" );
		}
		sb.append( " in (" );
		listExpressions.get( 0 ).appendHqlString( sb );
		for ( int i = 1; i < listExpressions.size(); i++ ) {
			sb.append( ", " );
			listExpressions.get( i ).appendHqlString( sb );
		}
		sb.append( ')' );
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmInListPredicate<>( testExpression, listExpressions, !isNegated(), nodeBuilder() );
	}
}
