/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.from.SqmCteJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.spi.NavigablePath;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedCteJoin<T> extends SqmCteJoin<T> implements SqmCorrelation<T, T>, SqmCorrelatedSingularValuedJoin<T, T> {

	private final SqmCorrelatedRootJoin<T> correlatedRootJoin;
	private final SqmCteJoin<T> correlationParent;

	public SqmCorrelatedCteJoin(SqmCteJoin<T> correlationParent) {
		//noinspection unchecked
		super(
				correlationParent.getCte(),
				correlationParent.getExplicitAlias(),
				correlationParent.getSqmJoinType(),
				(SqmRoot<T>) correlationParent.getRoot()
		);
		this.correlatedRootJoin = SqmCorrelatedDerivedRootJoin.create( correlationParent, this );
		this.correlationParent = correlationParent;
	}

	private SqmCorrelatedCteJoin(
			NavigablePath navigablePath,
			SqmCteStatement<T> cte,
			SqmPathSource<T> pathSource,
			String alias,
			SqmJoinType joinType,
			SqmRoot<T> sqmRoot,
			SqmCorrelatedRootJoin<T> correlatedRootJoin,
			SqmCteJoin<T> correlationParent) {
		super( navigablePath, cte, pathSource, alias, joinType, sqmRoot );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmCorrelatedCteJoin<T> copy(SqmCopyContext context) {
		final SqmCorrelatedCteJoin<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCorrelatedCteJoin<T> path = context.registerCopy(
				this,
				new SqmCorrelatedCteJoin<>(
						getNavigablePath(),
						getCte().copy( context ),
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

	@Override
	public SqmCteJoin<T> getCorrelationParent() {
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
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedCteJoin( this );
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof SqmCorrelatedCteJoin<?> that
			&& correlationParent.equals( that.correlationParent );
	}

	@Override
	public int hashCode() {
		return correlationParent.hashCode();
	}

	@Override
	public boolean isCompatible(Object other) {
		return other instanceof SqmCorrelatedCteJoin<?> that
			&& correlationParent.isCompatible( that.correlationParent );
	}

	@Override
	public int cacheHashCode() {
		return correlationParent.cacheHashCode();
	}

}
