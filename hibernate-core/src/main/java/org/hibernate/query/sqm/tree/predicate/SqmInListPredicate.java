/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Expression;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
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

	public SqmInListPredicate(
			SqmExpression<T> testExpression,
			NodeBuilder nodeBuilder,
			SqmExpression<T>... listExpressions) {
		this( testExpression, ArrayHelper.toExpandableList( listExpressions ), nodeBuilder );
	}

	public SqmInListPredicate(
			SqmExpression<T> testExpression,
			List<SqmExpression<T>> listExpressions,
			NodeBuilder nodeBuilder) {
		this( testExpression, listExpressions, false, nodeBuilder );
	}

	public SqmInListPredicate(
			SqmExpression<T> testExpression,
			List<SqmExpression<T>> listExpressions,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.testExpression = testExpression;
		this.listExpressions = listExpressions;
		for ( SqmExpression listExpression : listExpressions ) {
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
		addExpression( nodeBuilder().literal( value ) );
		return this;
	}

	@Override
	public SqmInPredicate<T> value(Expression value) {
		addExpression( (SqmExpression) value );
		return this;
	}

	@Override
	public SqmInPredicate<T> value(JpaExpression value) {
		addExpression( (SqmExpression) value );
		return this;
	}

	public List<SqmExpression<T>> getListExpressions() {
		return listExpressions;
	}

	public void addExpression(SqmExpression expression) {
		implyListElementType( expression );

		listExpressions.add( expression );
	}

	private void implyListElementType(SqmExpression expression) {
		expression.applyInferableType(
				QueryHelper.highestPrecedenceType2(
						getTestExpression().getExpressableType(),
						expression.getExpressableType()
				)
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitInListPredicate( this );
	}
}
