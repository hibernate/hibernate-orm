/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	public void appendHqlString(StringBuilder hql) {
		pluralPath.appendHqlString( hql );
		if ( isNegated() ) {
			hql.append( " is not empty" );
		}
		else {
			hql.append( " is empty" );
		}
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmEmptinessPredicate( pluralPath, !isNegated(), nodeBuilder() );
	}
}
