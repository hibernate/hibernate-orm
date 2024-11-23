/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;

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
		this.negated = !this.negated;
	}

	protected abstract SqmNegatablePredicate createNegatedNode();

	@Override
	public SqmNegatablePredicate not() {
		// in certain cases JPA required that this always return
		// a new instance.
		return createNegatedNode();
	}

}
