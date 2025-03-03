/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedPluralPartJoin<O, T> extends SqmPluralPartJoin<O, T> implements SqmCorrelatedJoin<O, T> {

	private final SqmCorrelatedRootJoin<O> correlatedRootJoin;
	private final SqmPluralPartJoin<O, T> correlationParent;

	public SqmCorrelatedPluralPartJoin(SqmPluralPartJoin<O, T> correlationParent) {
		super(
				correlationParent.getLhs(),
				correlationParent.getNavigablePath(),
				correlationParent.getReferencedPathSource(),
				correlationParent.getExplicitAlias(),
				SqmJoinType.INNER,
				correlationParent.nodeBuilder()
		);
		this.correlatedRootJoin = SqmCorrelatedRootJoin.create( correlationParent, this );
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmCorrelatedPluralPartJoin<O, T> copy(SqmCopyContext context) {
		final SqmCorrelatedPluralPartJoin<O, T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCorrelatedPluralPartJoin<O, T> path = context.registerCopy(
				this,
				new SqmCorrelatedPluralPartJoin<>( correlationParent.copy( context ) )
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedPluralPartJoin( this );
	}

	@Override
	public SqmPluralPartJoin<O, T> getCorrelationParent() {
		return correlationParent;
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return correlationParent;
	}

	@Override
	public boolean isCorrelated() {
		return true;
	}

	@Override
	public SqmRoot<O> getCorrelatedRoot() {
		return correlatedRootJoin;
	}

}
