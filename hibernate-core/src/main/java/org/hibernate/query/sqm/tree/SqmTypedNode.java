/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
