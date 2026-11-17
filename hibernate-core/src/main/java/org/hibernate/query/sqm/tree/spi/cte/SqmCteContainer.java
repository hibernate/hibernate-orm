package org.hibernate.query.sqm.tree.spi.cte;

import java.util.Collection;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.criteria.JpaCteContainer;
import org.hibernate.query.sqm.internal.CteSqmRenderContext;
import org.hibernate.query.sqm.tree.spi.SqmNode;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

/**
 * @author Christian Beikov
 */
public interface SqmCteContainer extends SqmNode, JpaCteContainer {

	@Nonnull
	Collection<SqmCteStatement<?>> getCteStatements();

	@Nullable
	SqmCteStatement<?> getCteStatement(@Nonnull String cteLabel);

	/** Render the WITH clause and return the context in which its CTE names are visible. */
	default SqmRenderContext appendHqlCteClause(StringBuilder hql, SqmRenderContext context) {
		final var statements = getCteStatements();
		if ( statements.isEmpty() ) {
			return context;
		}
		final var cteContext = new CteSqmRenderContext( context, this );
		String separator = "with ";
		for ( var cte : statements ) {
			hql.append( separator );
			cte.appendHqlString( hql, cteContext );
			separator = ", ";
		}
		hql.append( ' ' );
		return cteContext;
	}
}
