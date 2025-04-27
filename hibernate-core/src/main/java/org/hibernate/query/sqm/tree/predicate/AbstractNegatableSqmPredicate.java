/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNegatableSqmPredicate extends AbstractSqmPredicate implements SqmNegatablePredicate {
	private boolean negated;

	public AbstractNegatableSqmPredicate(NodeBuilder nodeBuilder) {
		this( false, nodeBuilder );
	}

	public AbstractNegatableSqmPredicate(boolean negated, NodeBuilder nodeBuilder) {
		this( nodeBuilder.getBooleanType(), negated, nodeBuilder );
	}

	public AbstractNegatableSqmPredicate(SqmExpressible<Boolean> type, boolean negated, NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		this.negated = negated;
	}

	@Override
	public boolean isNegated() {
		return negated;
	}

	@Override
	public void negate() {
		negated = !negated;
	}

	protected abstract SqmNegatablePredicate createNegatedNode();

	@Override
	public SqmNegatablePredicate not() {
		// in certain cases JPA required that this always return
		// a new instance.
		return createNegatedNode();
	}

	@Override
	// for safety only, overridden on all subtypes
	public boolean equals(Object other) {
		return other instanceof AbstractNegatableSqmPredicate that
			&& this.negated == that.negated
			&& this.getClass() == that.getClass()
			&& Objects.equals( this.toHqlString(), that.toHqlString() );
	}

	@Override
	// for safety only, overridden on all subtypes
	public int hashCode() {
		return Objects.hash( getClass(), negated );
	}
}
