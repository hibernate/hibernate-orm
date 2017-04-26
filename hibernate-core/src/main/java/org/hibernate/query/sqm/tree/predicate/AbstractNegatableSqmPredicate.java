/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNegatableSqmPredicate implements NegatableSqmPredicate {
	private boolean negated;

	public AbstractNegatableSqmPredicate() {
		this( false );
	}

	public AbstractNegatableSqmPredicate(boolean negated) {
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
}
