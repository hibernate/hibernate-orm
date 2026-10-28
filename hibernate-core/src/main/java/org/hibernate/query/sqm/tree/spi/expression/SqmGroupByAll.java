package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;


/**
 * @author Loïc Lefèvre
 */
public class SqmGroupByAll extends AbstractSqmExpression<Object> {

	public SqmGroupByAll(@Nonnull NodeBuilder builder) {
		super( null, builder );
	}

	@Nonnull
	@Override
	public SqmGroupByAll copy(@Nonnull SqmCopyContext context) {
		final SqmGroupByAll existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy( this, new SqmGroupByAll( nodeBuilder() ) );
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return null;
	}
	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( "all" );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmGroupByAll;
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
