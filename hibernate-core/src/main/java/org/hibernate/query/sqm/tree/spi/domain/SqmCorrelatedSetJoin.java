package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;

/**
 * @author Christian Beikov
 */
public class SqmCorrelatedSetJoin<O, T> extends SqmSetJoin<O, T> implements SqmCorrelatedJoin<O, T> {

	private final SqmCorrelatedRootJoin<O> correlatedRootJoin;
	private final SqmSetJoin<O, T> correlationParent;

	public SqmCorrelatedSetJoin(@Nonnull SqmSetJoin<O, T> correlationParent) {
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

	private SqmCorrelatedSetJoin(
			@Nonnull SqmFrom<?, O> lhs,
			@Nonnull SqmSetPersistentAttribute<O, T> attribute,
			@Nullable String alias,
			@Nonnull SqmJoinType sqmJoinType,
			boolean fetched,
			@Nonnull NodeBuilder nodeBuilder,
			@Nonnull SqmCorrelatedRootJoin<O> correlatedRootJoin,
			@Nonnull SqmSetJoin<O, T> correlationParent) {
		super( lhs, correlationParent.getNavigablePath(), attribute, alias, sqmJoinType, fetched, nodeBuilder );
		this.correlatedRootJoin = correlatedRootJoin;
		this.correlationParent = correlationParent;
	}

	@Nonnull
	@Override
	public SqmCorrelatedSetJoin<O, T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmCorrelatedSetJoin<>(
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

	@Nonnull
	@Override
	public SqmSetJoin<O, T> getCorrelationParent() {
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
	public SqmRoot<O> getCorrelatedRoot() {
		return correlatedRootJoin;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedSetJoin( this );
	}

	@Override
	public boolean deepEquals(@Nonnull SqmFrom<?, ?> other) {
		return super.deepEquals( other )
			&& other instanceof SqmCorrelatedSetJoin<?, ?> that
			&& correlationParent.equals( that.correlationParent );
	}

	@Override
	public boolean isDeepCompatible(@Nonnull SqmFrom<?, ?> other) {
		return super.isDeepCompatible( other )
			&& other instanceof SqmCorrelatedSetJoin<?, ?> that
			&& correlationParent.isCompatible( that.correlationParent );
	}
}
