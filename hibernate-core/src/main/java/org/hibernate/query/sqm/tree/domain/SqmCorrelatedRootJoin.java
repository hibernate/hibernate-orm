/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Steve Ebersole
 */
public class SqmCorrelatedRootJoin<T> extends SqmRoot<T> implements SqmCorrelation<T, T> {

	SqmCorrelatedRootJoin(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedNavigable,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, nodeBuilder );
	}

	public static <X, J extends SqmJoin<X, ?>> SqmCorrelatedRootJoin<X> create(J correlationParent, J correlatedJoin) {
		final SqmFrom<?, X> parentPath = (SqmFrom<?, X>) correlationParent.getParentPath();
		final SqmCorrelatedRootJoin<X> rootJoin;
		if ( parentPath == null ) {
			rootJoin = new SqmCorrelatedRootJoin(
					correlationParent.getNavigablePath(),
					correlationParent.getReferencedPathSource(),
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
		return walker.visitCorrelation( this );
	}
}
