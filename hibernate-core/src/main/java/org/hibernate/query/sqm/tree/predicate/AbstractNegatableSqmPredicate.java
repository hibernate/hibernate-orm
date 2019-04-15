/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.NodeBuilder;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNegatableSqmPredicate extends AbstractSqmPredicate implements SqmNegatablePredicate {
	private boolean negated;

	public AbstractNegatableSqmPredicate(NodeBuilder nodeBuilder) {
		this( false, nodeBuilder );
	}

	public AbstractNegatableSqmPredicate(boolean negated, NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.negated = negated;
	}

	@Override
	public boolean isNegated() {
		return negated;
	}

	@Override
	public void negate() {
		this.negated = !this.negated;
	}

	@Override
	public SqmNegatablePredicate not() {
		// in certain cases JPA required that this always return
		// a new instance.  we may need to allow for that here (compliance?)
		negate();
		return this;
	}

}
