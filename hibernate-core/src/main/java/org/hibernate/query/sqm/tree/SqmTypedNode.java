/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Optional contract for {@link SqmNode} implementations which are typed.
 *
 * @author Steve Ebersole
 */
public interface SqmTypedNode<T> extends SqmNode, SqmExpressibleAccessor<T>, SqmVisitableNode {
	/**
	 * The Java type descriptor for this node.
	 */
	default @Nullable JavaType<T> getNodeJavaType() {
		final SqmExpressible<T> nodeType = getNodeType();
		return nodeType == null ? null : nodeType.getExpressibleJavaType();
	}

	@Override
	default @Nullable SqmBindableType<T> getExpressible() {
		return getNodeType();
	}

	@Nullable
	SqmBindableType<T> getNodeType();

	@Override
	SqmTypedNode<T> copy(SqmCopyContext context);
}
