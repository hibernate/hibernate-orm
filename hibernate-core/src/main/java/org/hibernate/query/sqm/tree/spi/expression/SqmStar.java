package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

/**
 * @author Gavin King
 */
public class SqmStar extends AbstractSqmExpression<Object> {

	public SqmStar(@Nonnull NodeBuilder builder) {
		super( null, builder );
	}

	@Nonnull
	@Override
	public SqmStar copy(@Nonnull SqmCopyContext context) {
		final SqmStar existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy( this, new SqmStar( nodeBuilder() ) );
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitStar( this );
	}
	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( "*" );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmStar;
	}

	@Override
	public int hashCode() {
		return 1;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
