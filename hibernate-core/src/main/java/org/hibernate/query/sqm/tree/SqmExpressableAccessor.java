/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Accessor for {@link SqmExpressable}.
 *
 * @author Christian Beikov
 */
public interface SqmExpressableAccessor<T> {
	/**
	 * The Java type descriptor for this node.
	 */
	default JavaType<T> getNodeJavaType() {
		final SqmExpressable<T> nodeType = getExpressable();
		return nodeType != null ? nodeType.getExpressableJavaType() : null;
	}

	SqmExpressable<T> getExpressable();
}
