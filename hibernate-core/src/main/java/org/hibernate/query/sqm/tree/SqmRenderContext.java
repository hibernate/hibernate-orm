/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.sqm.internal.SimpleSqmRenderContext;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.from.SqmFrom;

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

	static SqmRenderContext simpleContext() {
		return new SimpleSqmRenderContext();
	}
}
