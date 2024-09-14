/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public class SqmGroupedPredicate extends AbstractSqmPredicate {
	private final SqmPredicate subPredicate;

	public SqmGroupedPredicate(SqmPredicate subPredicate, NodeBuilder nodeBuilder) {
		super( subPredicate.getExpressible(), nodeBuilder );
		this.subPredicate = subPredicate;
	}

	@Override
	public SqmGroupedPredicate copy(SqmCopyContext context) {
		final SqmGroupedPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmGroupedPredicate predicate = context.registerCopy(
				this,
				new SqmGroupedPredicate(
						subPredicate.copy( context ),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	public SqmPredicate getSubPredicate() {
		return subPredicate;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitGroupedPredicate( this );
	}

	@Override
	public boolean isNegated() {
		return false;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		final List<Expression<Boolean>> expressions = new ArrayList<>( 1 );
		expressions.add( subPredicate );
		return expressions;
	}

	@Override
	public SqmPredicate not() {
		return new SqmNegatedPredicate( this, nodeBuilder() );
	}
	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( '(' );
		subPredicate.appendHqlString( sb );
		sb.append( ')' );
	}
}
