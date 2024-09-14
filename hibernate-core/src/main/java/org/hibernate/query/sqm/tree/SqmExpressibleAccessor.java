/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
