package org.hibernate.query.sqm.tree.spi;

import org.hibernate.query.sqm.internal.SimpleSqmRenderContext;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.spi.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;

/**
 * Context used while rendering SQM nodes to HQL.
 *
 * @see SqmVisitableNode#appendHqlString(StringBuilder, SqmRenderContext)
 * @since 7.0
 */
public interface SqmRenderContext {

	/**
	 * Returns an alias for the given from node.
	 *
	 * @param from The from element
	 * @return The resolved alias
	 */
	String resolveAlias(SqmFrom<?, ?> from);

	String resolveParameterName(JpaCriteriaParameter<?> parameter);

	/** The HQL name used in both the declaration and references to this CTE. */
	default String resolveCteName(SqmCteStatement<?> cte) {
		final String name = cte.getCteTable().getCteName();
		return cte.getName() == null ? "generated_" + name : name;
	}

	static SqmRenderContext simpleContext() {
		return new SimpleSqmRenderContext();
	}
}
