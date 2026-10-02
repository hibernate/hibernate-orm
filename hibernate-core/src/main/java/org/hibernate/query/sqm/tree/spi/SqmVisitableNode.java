package org.hibernate.query.sqm.tree.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.query.sqm.spi.SemanticQueryWalker;

/**
 * Optional contract for SqmNode implementations that can be visited
 * by a SemanticQueryWalker.
 *
 * @author Steve Ebersole
 */
public interface SqmVisitableNode extends SqmNode {
	/**
	 * Accept the walker per visitation
	 */
	@Nullable
	<X> X accept(@Nonnull SemanticQueryWalker<X> walker);

	void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context);

	@Nonnull
	default String toHqlString() {
		final StringBuilder hql = new StringBuilder();
		appendHqlString( hql, SqmRenderContext.simpleContext() );
		return hql.toString();
	}
}
