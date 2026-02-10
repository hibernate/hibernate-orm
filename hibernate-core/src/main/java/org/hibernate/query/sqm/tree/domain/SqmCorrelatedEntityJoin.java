/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedEntityJoin<L,R> extends SqmEntityJoin<L,R> implements SqmCorrelatedSingularValuedJoin<L, R> {

	private final SqmCorrelatedRootJoin<L> correlatedRootJoin;
	private final SqmEntityJoin<L,R> correlationParent;

	public SqmCorrelatedEntityJoin(SqmEntityJoin<L,R> correlationParent) {
		super(
				correlationParent.getNavigablePath(),
				correlationParent.getReferencedPathSource(),
				correlationParent.getExplicitAlias(),
				SqmJoinType.INNER,
				correlationParent.getRoot()
		);
		this.correlatedRootJoin = SqmCorrelatedRootJoin.create( correlationParent, this );
		this.correlationParent = correlationParent;
	}

	public SqmCorrelatedEntityJoin(
			EntityDomainType<R> joinedEntityDescriptor,
			@Nullable String alias,
			SqmJoinType joinType,
			SqmRoot<L> sqmRoot,
			SqmCorrelatedRootJoin<L> correlatedRootJoin,
			SqmEntityJoin<L,R> correlationParent) {
		super( correlationParent.getNavigablePath(), joinedEntityDescriptor, alias, joinType, sqmRoot );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmCorrelatedEntityJoin<L,R> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmCorrelatedEntityJoin<>(
						getReferencedPathSource(),
						getExplicitAlias(),
						getSqmJoinType(),
						getRoot().copy( context ),
						correlatedRootJoin.copy( context ),
						correlationParent.copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmRoot<?> findRoot() {
		return getCorrelatedRoot();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedEntityJoin(this);
	}

	@Override
	public SqmEntityJoin<L,R> getCorrelationParent() {
		return correlationParent;
	}

	@Override
	public SqmPath<R> getWrappedPath() {
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
	public SqmCorrelatedEntityJoin<L,R> createCorrelation() {
		return new SqmCorrelatedEntityJoin<>( this );
	}

	@Override
	public SqmCorrelatedEntityJoin<L,R> makeCopy(SqmCreationProcessingState creationProcessingState) {
		final var pathRegistry = creationProcessingState.getPathRegistry();
		return new SqmCorrelatedEntityJoin<>(
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				pathRegistry.resolveFromByPath( getRoot().getNavigablePath() ),
				pathRegistry.resolveFromByPath( correlatedRootJoin.getNavigablePath() ),
				pathRegistry.resolveFromByPath( correlationParent.getNavigablePath() )
		);
	}

	@Override
	public boolean deepEquals(SqmFrom<?, ?> other) {
		return super.deepEquals( other )
			&& other instanceof SqmCorrelatedEntityJoin<?, ?> that
			&& correlationParent.equals( that.correlationParent );
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> other) {
		return super.isDeepCompatible( other )
			&& other instanceof SqmCorrelatedEntityJoin<?, ?> that
			&& correlationParent.isCompatible( that.correlationParent );
	}
}
