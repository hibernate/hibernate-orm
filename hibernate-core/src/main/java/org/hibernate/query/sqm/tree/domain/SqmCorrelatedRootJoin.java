/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmCorrelatedRootJoin<T> extends SqmRoot<T> implements SqmCorrelation<T, T> {

	public SqmCorrelatedRootJoin(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedNavigable,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, nodeBuilder );
	}

	@Override
	public SqmCorrelatedRootJoin<T> copy(SqmCopyContext context) {
		final SqmCorrelatedRootJoin<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCorrelatedRootJoin<T> path = context.registerCopy(
				this,
				new SqmCorrelatedRootJoin<>(
						getNavigablePath(),
						getReferencedPathSource(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	// Need to suppress argument warnings because correlatedJoin which is under initialization is passed to addSqmJoin,
	// which expects an initialized argument. We know this is safe though because we only store the instance
	@SuppressWarnings({"unchecked", "argument"})
	public static <X, J extends SqmJoin<X, ?>> SqmCorrelatedRootJoin<X> create(J correlationParent, @UnderInitialization J correlatedJoin) {
		final SqmFrom<?, X> parentPath = (SqmFrom<?, X>) correlationParent.getParentPath();
		final SqmCorrelatedRootJoin<X> rootJoin;
		if ( parentPath == null ) {
			rootJoin = new SqmCorrelatedRootJoin<>(
					correlationParent.getNavigablePath(),
					(SqmPathSource<X>) correlationParent.getReferencedPathSource(),
					correlationParent.nodeBuilder()
			);
		}
		else {
			rootJoin = new SqmCorrelatedRootJoin<>(
					parentPath.getNavigablePath(),
					parentPath.getReferencedPathSource(),
					correlationParent.nodeBuilder()
			);
		}
		rootJoin.addSqmJoin( correlatedJoin );
		return rootJoin;
	}

	@Override
	public SqmRoot<T> getCorrelationParent() {
		return this;
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return getCorrelationParent();
	}

	@Override
	public boolean isCorrelated() {
		return true;
	}

	@Override
	public SqmRoot<T> getCorrelatedRoot() {
		return this;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedRootJoin( this );
	}
}
