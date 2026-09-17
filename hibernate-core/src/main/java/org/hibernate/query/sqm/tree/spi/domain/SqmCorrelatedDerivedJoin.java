/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.from.SqmDerivedJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.query.sqm.tree.spi.select.SqmSubQuery;
import org.hibernate.spi.NavigablePath;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedDerivedJoin<T> extends SqmDerivedJoin<T> implements SqmCorrelation<T, T>, SqmCorrelatedSingularValuedJoin<T, T> {

	private final SqmCorrelatedRootJoin<T> correlatedRootJoin;
	private final SqmDerivedJoin<T> correlationParent;

	public SqmCorrelatedDerivedJoin(@Nonnull SqmDerivedJoin<T> correlationParent) {
		//noinspection unchecked
		super(
				correlationParent.getNavigablePath(),
				correlationParent.getQueryPart(),
				correlationParent.isLateral(),
				correlationParent.getReferencedPathSource(),
				correlationParent.getExplicitAlias(),
				correlationParent.getSqmJoinType(),
				(SqmRoot<T>) correlationParent.getRoot()
		);
		this.correlatedRootJoin = SqmCorrelatedDerivedRootJoin.create( correlationParent, this );
		this.correlationParent = correlationParent;
	}

	private SqmCorrelatedDerivedJoin(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmSubQuery<T> subQuery,
			boolean lateral,
			@Nonnull SqmPathSource<T> pathSource,
			@Nullable String alias,
			@Nonnull SqmJoinType joinType,
			@Nonnull SqmRoot<T> sqmRoot,
			@Nonnull SqmCorrelatedRootJoin<T> correlatedRootJoin,
			@Nonnull SqmDerivedJoin<T> correlationParent) {
		super( navigablePath, subQuery, lateral, pathSource, alias, joinType, sqmRoot );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Nonnull
	@Override
	public SqmCorrelatedDerivedJoin<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmCorrelatedDerivedJoin<>(
						getNavigablePath(),
						getQueryPart(),
						isLateral(),
						getReferencedPathSource(),
						getExplicitAlias(),
						getSqmJoinType(),
						(SqmRoot<T>) findRoot().copy( context ),
						correlatedRootJoin.copy( context ),
						correlationParent.copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	@Nonnull
	@Override
	public SqmDerivedJoin<T> getCorrelationParent() {
		return correlationParent;
	}

	@Nonnull
	@Override
	public SqmPath<T> getWrappedPath() {
		return correlationParent;
	}

	@Override
	public boolean isCorrelated() {
		return true;
	}

	@Nonnull
	@Override
	public SqmRoot<T> getCorrelatedRoot() {
		return correlatedRootJoin;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedDerivedJoin( this );
	}

	@Override
	public boolean deepEquals(@Nonnull SqmFrom<?, ?> other) {
		return super.deepEquals( other )
			&& other instanceof SqmCorrelatedDerivedJoin<?> that
			&& correlationParent.equals( that.correlationParent );
	}

	@Override
	public boolean isDeepCompatible(@Nonnull SqmFrom<?, ?> other) {
		return super.isDeepCompatible( other )
			&& other instanceof SqmCorrelatedDerivedJoin<?> that
			&& correlationParent.isCompatible( that.correlationParent );
	}

}
