package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

/**
 * @author Steve Ebersole
 */
public class SqmNumericExpressionWrapper<N extends Number & Comparable<N>>
		extends AbstractSqmExpression<N>
		implements SqmNumericExpressionImplementor<N>, SqmExpressionWrapper<N> {
	private final SqmExpression<N> wrappedExpression;

	public SqmNumericExpressionWrapper(@Nonnull SqmExpression<N> wrappedExpression) {
		super( wrappedExpression.getNodeType(), wrappedExpression.nodeBuilder() );
		this.wrappedExpression = wrappedExpression;
	}

	@Nonnull
	@Override
	public SqmExpression<N> getWrappedExpression() {
		return wrappedExpression;
	}

	@Nonnull
	@Override
	public SqmNumericExpression<N> coalesce(@Nonnull Expression<? extends N> y) {
		var expr = nodeBuilder().coalesce( this, y);
		return new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	public SqmNumericExpression<N> coalesce(@Nonnull N y) {
		var expr = nodeBuilder().coalesce( this, y);
		return new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	public SqmNumericExpression<N> nullif(@Nonnull Expression<? extends N> y) {
		var expr = nodeBuilder().nullif( this, y);
		return new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	public SqmNumericExpression<N> nullif(@Nonnull N y) {
		var expr = nodeBuilder().nullif( this, y);
		return new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	public SqmExpression<N> copy(@Nonnull SqmCopyContext context) {
		return wrappedExpression.copy( context );
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return wrappedExpression.accept( walker );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		wrappedExpression.appendHqlString( hql, context );
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmNumericExpressionWrapper<?> that
			&& getClass() == that.getClass()
			&& wrappedExpression.equals( that.wrappedExpression );
	}

	@Override
	public int hashCode() {
		return wrappedExpression.hashCode();
	}
}
