/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;

/**
 * Models any aliased expression.  E.g. `select exp as e ...`
 * where  the aliased node is the complete `exp as e` "expression" -
 * `exp` is it's "wrapped node" and `e` is the alias.
 *
 * This will only ever be some kind of selection (dynamic-instantiation,
 * JPA select-object syntax, an expression or a dynamic-instantiation
 * argument).  Each of these can be represented as a
 * {@link DomainResultProducer} which is ultimately
 *
 * @author Steve Ebersole
 */
public interface SqmAliasedNode<T> extends SqmTypedNode<T> {
	SqmSelectableNode<T> getSelectableNode();
	@Nullable String getAlias();

	@Override
	default @Nullable SqmBindableType<T> getNodeType() {
		return getSelectableNode().getNodeType();
	}

	@Override
	default @Nullable SqmBindableType<T> getExpressible() {
		return getSelectableNode().getExpressible();
	}
}
