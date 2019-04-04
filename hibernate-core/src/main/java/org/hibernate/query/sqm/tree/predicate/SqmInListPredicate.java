/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmInListPredicate extends AbstractNegatableSqmPredicate implements SqmInPredicate {
	private final SqmExpression testExpression;
	private final List<SqmExpression> listExpressions;

	public SqmInListPredicate(SqmExpression testExpression) {
		this( testExpression, new ArrayList<>() );
	}

	public SqmInListPredicate(SqmExpression testExpression, SqmExpression... listExpressions) {
		this( testExpression, ArrayHelper.toExpandableList( listExpressions ) );
	}

	public SqmInListPredicate(
			SqmExpression testExpression,
			List<SqmExpression> listExpressions) {
		this( testExpression, listExpressions, false );
	}

	public SqmInListPredicate(
			SqmExpression testExpression,
			List<SqmExpression> listExpressions,
			boolean negated) {
		super( negated );
		this.testExpression = testExpression;
		this.listExpressions = listExpressions;
		for ( SqmExpression listExpression : listExpressions ) {
			implyListElementType( listExpression );
		}

	}

	@Override
	public SqmExpression getTestExpression() {
		return testExpression;
	}

	public List<SqmExpression> getListExpressions() {
		return listExpressions;
	}

	public void addExpression(SqmExpression expression) {
		implyListElementType( expression );

		listExpressions.add( expression );
	}

	private void implyListElementType(SqmExpression expression) {
		expression.applyInferableType(
				QueryHelper.highestPrecedenceType(
						getTestExpression().getExpressableType(),
						expression.getExpressableType()
				)
		);
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitInListPredicate( this );
	}
}
