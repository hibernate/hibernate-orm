/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedCrossJoin<T> extends SqmCrossJoin<T> implements SqmCorrelation<T, T> {

	private final SqmCorrelatedRootJoin<T> correlatedRootJoin;
	private final SqmCrossJoin<T> correlationParent;

	public SqmCorrelatedCrossJoin(SqmCrossJoin<T> correlationParent) {
		super(
				correlationParent.getNavigablePath(),
				correlationParent.getReferencedPathSource(),
				correlationParent.getExplicitAlias(),
				correlationParent.getRoot()
		);
		this.correlatedRootJoin = SqmCorrelatedRootJoin.create( correlationParent, this );
		this.correlationParent = correlationParent;
	}

	private SqmCorrelatedCrossJoin(
			SqmEntityDomainType<T> joinedEntityDescriptor,
			@Nullable String alias,
			SqmRoot<?> sqmRoot,
			SqmCorrelatedRootJoin<T> correlatedRootJoin,
			SqmCrossJoin<T> correlationParent) {
		super( correlationParent.getNavigablePath(), joinedEntityDescriptor, alias, sqmRoot );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmCorrelatedCrossJoin<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmCorrelatedCrossJoin<>(
						getReferencedPathSource(),
						getExplicitAlias(),
						getRoot().copy( context ),
						correlatedRootJoin.copy( context ),
						correlationParent.copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmCrossJoin<T> getCorrelationParent() {
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
	public SqmRoot<T> getCorrelatedRoot() {
		return correlatedRootJoin;
	}

	@Override
	public SqmCorrelatedCrossJoin<T> makeCopy(SqmCreationProcessingState creationProcessingState) {
		final var pathRegistry = creationProcessingState.getPathRegistry();
		return new SqmCorrelatedCrossJoin<>(
				getReferencedPathSource(),
				getExplicitAlias(),
				pathRegistry.resolveFromByPath( getRoot().getNavigablePath() ),
				pathRegistry.resolveFromByPath( correlatedRootJoin.getNavigablePath() ),
				pathRegistry.resolveFromByPath( correlationParent.getNavigablePath() )
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedCrossJoin( this );
	}

	@Override
	public boolean deepEquals(SqmFrom<?, ?> other) {
		return super.deepEquals( other )
			&& other instanceof SqmCorrelatedCrossJoin<?> that
			&& correlationParent.equals( that.correlationParent );
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> other) {
		return super.isDeepCompatible( other )
			&& other instanceof SqmCorrelatedCrossJoin<?> that
			&& correlationParent.isCompatible( that.correlationParent );
	}

}
