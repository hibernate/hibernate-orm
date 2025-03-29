/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

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
	default JavaType<T> getNodeJavaType() {
		final SqmExpressible<T> nodeType = getExpressible();
		return nodeType != null ? nodeType.getExpressibleJavaType() : null;
	}

	SqmExpressible<T> getExpressible();
}
