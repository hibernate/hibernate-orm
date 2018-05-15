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
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class InListSqmPredicate extends AbstractNegatableSqmPredicate implements InSqmPredicate {
	private final SqmExpression testExpression;
	private final List<SqmExpression> listExpressions;

	public InListSqmPredicate(SqmExpression testExpression) {
		this( testExpression, new ArrayList<>() );
	}

	public InListSqmPredicate(SqmExpression testExpression, SqmExpression... listExpressions) {
		this( testExpression, ArrayHelper.toExpandableList( listExpressions ) );
	}

	public InListSqmPredicate(
			SqmExpression testExpression,
			List<SqmExpression> listExpressions) {
		this( testExpression, listExpressions, false );
	}

	public InListSqmPredicate(
			SqmExpression testExpression,
			List<SqmExpression> listExpressions,
			boolean negated) {
		super( negated );
		this.testExpression = testExpression;
		this.listExpressions = listExpressions;
	}

	@Override
	public SqmExpression getTestExpression() {
		return testExpression;
	}

	public List<SqmExpression> getListExpressions() {
		return listExpressions;
	}

	public void addExpression(SqmExpression expression) {
		listExpressions.add( expression );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitInListPredicate( this );
	}
}
