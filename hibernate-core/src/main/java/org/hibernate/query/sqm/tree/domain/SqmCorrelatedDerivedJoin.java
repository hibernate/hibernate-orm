/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmDerivedJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.spi.NavigablePath;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedDerivedJoin<T> extends SqmDerivedJoin<T> implements SqmCorrelation<T, T> {

	private final SqmCorrelatedRootJoin<T> correlatedRootJoin;
	private final SqmDerivedJoin<T> correlationParent;

	public SqmCorrelatedDerivedJoin(SqmDerivedJoin<T> correlationParent) {
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
			NavigablePath navigablePath,
			SqmSubQuery<T> subQuery,
			boolean lateral,
			SqmPathSource<T> pathSource,
			String alias,
			SqmJoinType joinType,
			SqmRoot<T> sqmRoot,
			SqmCorrelatedRootJoin<T> correlatedRootJoin,
			SqmDerivedJoin<T> correlationParent) {
		super( navigablePath, subQuery, lateral, pathSource, alias, joinType, sqmRoot );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmCorrelatedDerivedJoin<T> copy(SqmCopyContext context) {
		final SqmCorrelatedDerivedJoin<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCorrelatedDerivedJoin<T> path = context.registerCopy(
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

	@Override
	public SqmDerivedJoin<T> getCorrelationParent() {
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
		return walker.visitCorrelatedDerivedJoin( this );
	}

}
