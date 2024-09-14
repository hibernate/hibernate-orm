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
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;

/**
 * @author Steve Ebersole
 */
public class SqmEmptinessPredicate extends AbstractNegatableSqmPredicate {
	private final SqmPluralValuedSimplePath<?> pluralPath;

	public SqmEmptinessPredicate(
			SqmPluralValuedSimplePath pluralPath,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.pluralPath = pluralPath;
	}

	@Override
	public SqmEmptinessPredicate copy(SqmCopyContext context) {
		final SqmEmptinessPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmEmptinessPredicate predicate = context.registerCopy(
				this,
				new SqmEmptinessPredicate(
						pluralPath.copy( context ),
						isNegated(),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	public SqmPluralValuedSimplePath<?> getPluralPath() {
		return pluralPath;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitIsEmptyPredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		pluralPath.appendHqlString( sb );
		if ( isNegated() ) {
			sb.append( " is not empty" );
		}
		else {
			sb.append( " is empty" );
		}
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmEmptinessPredicate( pluralPath, !isNegated(), nodeBuilder() );
	}
}
