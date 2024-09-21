/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Optional contract for SqmNode implementations which are
 * typed
 *
 * @author Steve Ebersole
 */
public interface SqmTypedNode<T> extends SqmNode, SqmExpressibleAccessor<T>, SqmVisitableNode {
	/**
	 * The Java type descriptor for this node.
	 */
	default @Nullable JavaType<T> getNodeJavaType() {
		final SqmExpressible<T> nodeType = getNodeType();
		return nodeType != null ? nodeType.getExpressibleJavaType() : null;
	}

	@Override
	default @Nullable SqmExpressible<T> getExpressible() {
		return getNodeType();
	}

	@Nullable SqmExpressible<T> getNodeType();

	@Override
	SqmTypedNode<T> copy(SqmCopyContext context);
}
