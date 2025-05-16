/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.Collection;
import java.util.Objects;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * @author Steve Ebersole
 */
public class SqmWhereClause implements SqmPredicateCollection {
	private final NodeBuilder nodeBuilder;

	private SqmPredicate predicate;

	public SqmWhereClause(NodeBuilder nodeBuilder) {
		this.nodeBuilder = nodeBuilder;
	}

	public SqmWhereClause(SqmPredicate predicate, NodeBuilder nodeBuilder) {
		this.nodeBuilder = nodeBuilder;
		this.predicate = predicate;
	}

	public SqmWhereClause copy(SqmCopyContext context) {
		return new SqmWhereClause(
				predicate == null ? null : predicate.copy( context ),
				nodeBuilder
		);
	}

	@Override
	public SqmPredicate getPredicate() {
		return predicate;
	}

	@Override
	public void setPredicate(SqmPredicate predicate) {
		this.predicate = predicate;
	}

	@Override
	public void applyPredicate(SqmPredicate predicate) {
		if ( this.predicate == null ) {
			this.predicate = predicate;
		}
		else {
			this.predicate = nodeBuilder.and( this.predicate, predicate );
		}
	}

	@Override
	public void applyPredicates(SqmPredicate... predicates) {
		for ( SqmPredicate sqmPredicate : predicates ) {
			applyPredicate( sqmPredicate );
		}
	}

	@Override
	public void applyPredicates(Collection<SqmPredicate> predicates) {
		for ( SqmPredicate sqmPredicate : predicates ) {
			applyPredicate( sqmPredicate );
		}
	}

	@Override
	public String toString() {
		return "where " + predicate;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof SqmWhereClause that
			&& Objects.equals( this.predicate, that.predicate );
	}

	@Override
	public int hashCode() {
		return predicate == null ? 0 : predicate.hashCode();
	}
}
