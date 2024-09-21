/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
