/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedEntityJoin<T> extends SqmEntityJoin<T> implements SqmCorrelation<T, T> {

	private final SqmCorrelatedRootJoin<T> correlatedRootJoin;
	private final SqmEntityJoin<T> correlationParent;

	public SqmCorrelatedEntityJoin(SqmEntityJoin<T> correlationParent) {
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
			EntityDomainType<T> joinedEntityDescriptor,
			String alias,
			SqmJoinType joinType,
			SqmRoot<?> sqmRoot,
			SqmCorrelatedRootJoin<T> correlatedRootJoin,
			SqmEntityJoin<T> correlationParent) {
		super( correlationParent.getNavigablePath(), joinedEntityDescriptor, alias, joinType, sqmRoot );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmCorrelatedEntityJoin<T> copy(SqmCopyContext context) {
		final SqmCorrelatedEntityJoin<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCorrelatedEntityJoin<T> path = context.registerCopy(
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
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedEntityJoin(this);
	}

	@Override
	public SqmEntityJoin<T> getCorrelationParent() {
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
	public SqmCorrelatedEntityJoin<T> makeCopy(SqmCreationProcessingState creationProcessingState) {
		final SqmPathRegistry pathRegistry = creationProcessingState.getPathRegistry();
		return new SqmCorrelatedEntityJoin<>(
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				pathRegistry.findFromByPath( getRoot().getNavigablePath() ),
				pathRegistry.findFromByPath( correlatedRootJoin.getNavigablePath() ),
				pathRegistry.findFromByPath( correlationParent.getNavigablePath() )
		);
	}

}
