/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
