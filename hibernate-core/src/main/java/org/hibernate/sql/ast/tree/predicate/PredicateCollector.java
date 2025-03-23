/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.function.Consumer;

public class PredicateCollector implements Consumer<Predicate> {
	private Predicate predicate;

	public PredicateCollector() {
	}

	public PredicateCollector(Predicate predicate) {
		this.predicate = predicate;
	}

	public void applyPredicate(Predicate incomingPredicate) {
		this.predicate = Predicate.combinePredicates( this.predicate, incomingPredicate );
	}

	@Override
	public void accept(Predicate predicate) {
		applyPredicate( predicate );
	}

	public Predicate getPredicate() {
		return predicate;
	}
}
