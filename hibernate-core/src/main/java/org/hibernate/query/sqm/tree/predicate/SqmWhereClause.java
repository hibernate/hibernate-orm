/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.Collection;

import org.hibernate.query.sqm.NodeBuilder;

/**
 * @author Steve Ebersole
 */
public class SqmWhereClause {
	private final NodeBuilder nodeBuilder;

	private SqmPredicate predicate;

	public SqmWhereClause(NodeBuilder nodeBuilder) {
		this.nodeBuilder = nodeBuilder;
	}

	public SqmWhereClause(SqmPredicate predicate, NodeBuilder nodeBuilder) {
		this.nodeBuilder = nodeBuilder;
		this.predicate = predicate;
	}

	public SqmPredicate getPredicate() {
		return predicate;
	}

	public void setPredicate(SqmPredicate predicate) {
		this.predicate = predicate;
	}

	public void applyPredicate(SqmPredicate predicate) {
		if ( this.predicate == null ) {
			this.predicate = predicate;
		}
		else {
			this.predicate = nodeBuilder.and( this.predicate, predicate );
		}
	}

	public void applyPredicates(SqmPredicate... predicates) {
		for ( SqmPredicate sqmPredicate : predicates ) {
			applyPredicate( sqmPredicate );
		}
	}

	public void applyPredicates(Collection<SqmPredicate> predicates) {
		for ( SqmPredicate sqmPredicate : predicates ) {
			applyPredicate( sqmPredicate );
		}
	}

	@Override
	public String toString() {
		return "where " + predicate;
	}
}
