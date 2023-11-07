/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedSingularJoin<O, T> extends SqmSingularJoin<O, T> implements SqmCorrelatedSingularValuedJoin<O, T> {

	private final SqmCorrelatedRootJoin<O> correlatedRootJoin;
	private final SqmSingularJoin<O, T> correlationParent;

	public SqmCorrelatedSingularJoin(SqmSingularJoin<O, T> correlationParent) {
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

	private SqmCorrelatedSingularJoin(
			SqmFrom<?, O> lhs,
			SingularPersistentAttribute<O, T> joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder,
			SqmCorrelatedRootJoin<O> correlatedRootJoin,
			SqmSingularJoin<O, T> correlationParent) {
		super( lhs, correlationParent.getNavigablePath(), joinedNavigable, alias, joinType, fetched, nodeBuilder );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmCorrelatedSingularJoin<O, T> copy(SqmCopyContext context) {
		final SqmCorrelatedSingularJoin<O, T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCorrelatedSingularJoin<O, T> path = context.registerCopy(
				this,
				new SqmCorrelatedSingularJoin<>(
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
	public SqmSingularJoin<O, T> getCorrelationParent() {
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
	public SqmCorrelatedSingularJoin<O, T> makeCopy(SqmCreationProcessingState creationProcessingState) {
		final SqmPathRegistry pathRegistry = creationProcessingState.getPathRegistry();
		return new SqmCorrelatedSingularJoin<>(
				pathRegistry.findFromByPath( getLhs().getNavigablePath() ),
				getAttribute(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder(),
				pathRegistry.findFromByPath( correlatedRootJoin.getNavigablePath() ),
				pathRegistry.findFromByPath( correlationParent.getNavigablePath() )
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedSingularJoin( this );
	}
}
