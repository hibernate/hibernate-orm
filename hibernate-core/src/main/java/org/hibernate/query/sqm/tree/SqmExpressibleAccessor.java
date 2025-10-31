/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Accessor for {@link SqmExpressible}.
 *
 * @author Christian Beikov
 */
public interface SqmExpressibleAccessor<T> {
	/**
	 * The Java type descriptor for this node.
	 */
	default @Nullable JavaType<T> getNodeJavaType() {
		final SqmExpressible<T> nodeType = getExpressible();
		return nodeType == null ? null : nodeType.getExpressibleJavaType();
	}

	@Nullable SqmBindableType<T> getExpressible();
}
