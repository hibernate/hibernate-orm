/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.criteria.Expression;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

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

	@SuppressWarnings("WeakerAccess")
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
			( (Collection<T>) value ).forEach(
					v -> addExpression( nodeBuilder().value( v, testExpression ) )
			);
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
		expression.applyInferableType(
				QueryHelper.highestPrecedenceType2(
						getTestExpression().getNodeType(),
						expression.getNodeType()
				)
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
}
