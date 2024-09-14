/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Gavin King
 */
public class SqmTruthnessPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> expression;
	private final boolean value;

	public SqmTruthnessPredicate(SqmExpression<?> expression, boolean value, boolean negated, NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.expression = expression;
		this.value = value;
	}

	public boolean getBooleanValue() {
		return value;
	}

	@Override
	public SqmTruthnessPredicate copy(SqmCopyContext context) {
		final SqmTruthnessPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTruthnessPredicate predicate = context.registerCopy(
				this,
				new SqmTruthnessPredicate(
						expression.copy( context ),
						getBooleanValue(),
						isNegated(),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	public SqmExpression<?> getExpression() {
		return expression;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitIsTruePredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		expression.appendHqlString( sb );
		sb.append(" is ");
		if ( isNegated() ) {
			sb.append( "not " );
		}
		sb.append( getBooleanValue() );
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmTruthnessPredicate( expression, getBooleanValue(), !isNegated(), nodeBuilder() );
	}
}
