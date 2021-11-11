/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmMemberOfPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression leftHandExpression;
	private final SqmPath<?> pluralPath;

	public SqmMemberOfPredicate(SqmExpression leftHandExpression, SqmPath<?> pluralPath, NodeBuilder nodeBuilder) {
		this( leftHandExpression, pluralPath, false, nodeBuilder );
	}

	@SuppressWarnings("WeakerAccess")
	public SqmMemberOfPredicate(
			SqmExpression leftHandExpression,
			SqmPath pluralPath,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );

		this.pluralPath = pluralPath;
		this.leftHandExpression = leftHandExpression;
	}

	public SqmExpression getLeftHandExpression() {
		return leftHandExpression;
	}

	public SqmPath<?> getPluralPath() {
		return pluralPath;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMemberOfPredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		leftHandExpression.appendHqlString( sb );
		if ( isNegated() ) {
			sb.append( " not" );
		}
		sb.append( " member of " );
		pluralPath.appendHqlString( sb );
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmMemberOfPredicate( leftHandExpression, pluralPath, !isNegated(), nodeBuilder() );
	}
}
