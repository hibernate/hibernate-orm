package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedEntityJoin<L,R> extends SqmEntityJoin<L,R> implements SqmCorrelatedSingularValuedJoin<L, R> {

	private final SqmCorrelatedRootJoin<L> correlatedRootJoin;
	private final SqmEntityJoin<L,R> correlationParent;

	public SqmCorrelatedEntityJoin(@Nonnull SqmEntityJoin<L,R> correlationParent) {
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
			@Nonnull EntityDomainType<R> joinedEntityDescriptor,
			@Nullable String alias,
			@Nonnull SqmJoinType joinType,
			@Nonnull SqmRoot<L> sqmRoot,
			@Nonnull SqmCorrelatedRootJoin<L> correlatedRootJoin,
			@Nonnull SqmEntityJoin<L,R> correlationParent) {
		super( correlationParent.getNavigablePath(), joinedEntityDescriptor, alias, joinType, sqmRoot );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Nonnull
	@Override
	public SqmCorrelatedEntityJoin<L,R> copy(@Nonnull SqmCopyContext context) {
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
	@Nonnull
	public SqmRoot<?> findRoot() {
		return getCorrelatedRoot();
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedEntityJoin(this);
	}

	@Nonnull
	@Override
	public SqmEntityJoin<L,R> getCorrelationParent() {
		return correlationParent;
	}

	@Nonnull
	@Override
	public SqmPath<R> getWrappedPath() {
		return correlationParent;
	}

	@Override
	public boolean isCorrelated() {
		return true;
	}

	@Nonnull
	@Override
	public SqmRoot<L> getCorrelatedRoot() {
		return correlatedRootJoin;
	}

	@Override
	@Nonnull
	public SqmCorrelatedEntityJoin<L,R> createCorrelation() {
		return new SqmCorrelatedEntityJoin<>( this );
	}

	@Nonnull
	@Override
	public SqmCorrelatedEntityJoin<L,R> makeCopy(@Nonnull SqmCreationProcessingState creationProcessingState) {
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
	public boolean deepEquals(@Nonnull SqmFrom<?, ?> other) {
		return super.deepEquals( other )
			&& other instanceof SqmCorrelatedEntityJoin<?, ?> that
			&& correlationParent.equals( that.correlationParent );
	}

	@Override
	public boolean isDeepCompatible(@Nonnull SqmFrom<?, ?> other) {
		return super.isDeepCompatible( other )
			&& other instanceof SqmCorrelatedEntityJoin<?, ?> that
			&& correlationParent.isCompatible( that.correlationParent );
	}
}
