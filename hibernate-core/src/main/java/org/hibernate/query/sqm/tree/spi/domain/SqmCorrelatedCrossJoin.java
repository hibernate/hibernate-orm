package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedCrossJoin<L, T> extends SqmCrossJoin<L, T> implements SqmCorrelation<L, T> {

	private final SqmCorrelatedRootJoin<L> correlatedRootJoin;
	private final SqmCrossJoin<L, T> correlationParent;

	public SqmCorrelatedCrossJoin(@Nonnull SqmCrossJoin<L, T> correlationParent) {
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
			@Nonnull SqmEntityDomainType<T> joinedEntityDescriptor,
			@Nullable String alias,
			@Nonnull SqmRoot<L> sqmRoot,
			@Nonnull SqmCorrelatedRootJoin<L> correlatedRootJoin,
			@Nonnull SqmCrossJoin<L, T> correlationParent) {
		super( correlationParent.getNavigablePath(), joinedEntityDescriptor, alias, sqmRoot );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Nonnull
	@Override
	public SqmCorrelatedCrossJoin<L, T> copy(@Nonnull SqmCopyContext context) {
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

	@Nonnull
	@Override
	public SqmCrossJoin<L, T> getCorrelationParent() {
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
	public SqmRoot<L> getCorrelatedRoot() {
		return correlatedRootJoin;
	}

	@Nonnull
	@Override
	public SqmCorrelatedCrossJoin<L, T> makeCopy(@Nonnull SqmCreationProcessingState creationProcessingState) {
		final var pathRegistry = creationProcessingState.getPathRegistry();
		return new SqmCorrelatedCrossJoin<>(
				getReferencedPathSource(),
				getExplicitAlias(),
				pathRegistry.resolveFromByPath( getRoot().getNavigablePath() ),
				pathRegistry.resolveFromByPath( correlatedRootJoin.getNavigablePath() ),
				pathRegistry.resolveFromByPath( correlationParent.getNavigablePath() )
		);
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedCrossJoin( this );
	}

	@Override
	public boolean deepEquals(@Nonnull SqmFrom<?, ?> other) {
		return super.deepEquals( other )
			&& other instanceof SqmCorrelatedCrossJoin<?, ?> that
			&& correlationParent.equals( that.correlationParent );
	}

	@Override
	public boolean isDeepCompatible(@Nonnull SqmFrom<?, ?> other) {
		return super.isDeepCompatible( other )
			&& other instanceof SqmCorrelatedCrossJoin<?, ?> that
			&& correlationParent.isCompatible( that.correlationParent );
	}

}
