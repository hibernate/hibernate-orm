/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedMapJoin<L,K,V> extends SqmMapJoin<L,K,V> implements SqmCorrelatedJoin<L,V> {

	private final SqmCorrelatedRootJoin<L> correlatedRootJoin;
	private final SqmMapJoin<L, K, V> correlationParent;

	public SqmCorrelatedMapJoin(SqmMapJoin<L, K, V> correlationParent) {
		super(
				correlationParent.getLhs(),
				correlationParent.getNavigablePath(),
				correlationParent.getAttribute(),
				correlationParent.getExplicitAlias(),
				SqmJoinType.INNER,
				false,
				correlationParent.nodeBuilder()
		);
		this.correlatedRootJoin = SqmCorrelatedRootJoin.create( correlationParent, this );
		this.correlationParent = correlationParent;
	}

	private SqmCorrelatedMapJoin(
			SqmFrom<?, L> lhs,
			SqmMapPersistentAttribute<L,K,V> attribute,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder,
			SqmCorrelatedRootJoin<L> correlatedRootJoin,
			SqmMapJoin<L, K, V> correlationParent) {
		super( lhs, correlationParent.getNavigablePath(), attribute, alias, sqmJoinType, fetched, nodeBuilder );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmCorrelatedMapJoin<L, K, V> copy(SqmCopyContext context) {
		final SqmCorrelatedMapJoin<L, K, V> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCorrelatedMapJoin<L, K, V> path = context.registerCopy(
				this,
				new SqmCorrelatedMapJoin<>(
						getLhs().copy( context ),
						getAttribute(),
						getExplicitAlias(),
						getSqmJoinType(),
						isFetched(),
						nodeBuilder(),
						correlatedRootJoin.copy( context ),
						correlationParent.copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmMapJoin<L, K, V> getCorrelationParent() {
		return correlationParent;
	}

	@Override
	public SqmPath<V> getWrappedPath() {
		return correlationParent;
	}

	@Override
	public boolean isCorrelated() {
		return true;
	}

	@Override
	public SqmRoot<L> getCorrelatedRoot() {
		return correlatedRootJoin;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedMapJoin( this );
	}
}
