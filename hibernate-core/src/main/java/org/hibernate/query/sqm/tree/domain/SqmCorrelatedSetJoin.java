/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedSetJoin<O, T> extends SqmSetJoin<O, T> implements SqmCorrelatedJoin<O, T> {

	private final SqmCorrelatedRootJoin<O> correlatedRootJoin;
	private final SqmSetJoin<O, T> correlationParent;

	public SqmCorrelatedSetJoin(SqmSetJoin<O, T> correlationParent) {
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

	private SqmCorrelatedSetJoin(
			SqmFrom<?, O> lhs,
			SqmSetPersistentAttribute<O, T> attribute,
			@Nullable String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder,
			SqmCorrelatedRootJoin<O> correlatedRootJoin,
			SqmSetJoin<O, T> correlationParent) {
		super( lhs, correlationParent.getNavigablePath(), attribute, alias, sqmJoinType, fetched, nodeBuilder );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmCorrelatedSetJoin<O, T> copy(SqmCopyContext context) {
		final SqmCorrelatedSetJoin<O, T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCorrelatedSetJoin<O, T> path = context.registerCopy(
				this,
				new SqmCorrelatedSetJoin<>(
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
	public SqmSetJoin<O, T> getCorrelationParent() {
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

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedSetJoin( this );
	}

	@Override
	public boolean deepEquals(SqmFrom<?, ?> other) {
		return super.deepEquals( other )
			&& other instanceof SqmCorrelatedSetJoin<?, ?> that
			&& correlationParent.equals( that.correlationParent );
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> other) {
		return super.isDeepCompatible( other )
			&& other instanceof SqmCorrelatedSetJoin<?, ?> that
			&& correlationParent.isCompatible( that.correlationParent );
	}
}
