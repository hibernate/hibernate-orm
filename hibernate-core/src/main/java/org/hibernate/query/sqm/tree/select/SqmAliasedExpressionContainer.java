/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmAliasedExpressionContainer<T extends SqmAliasedNode<?>> {
	T add(SqmExpression<?> expression, String alias);
	void add(T aliasExpression);
}
