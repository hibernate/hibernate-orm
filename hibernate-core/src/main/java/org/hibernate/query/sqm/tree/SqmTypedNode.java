/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Optional contract for SqmNode implementations which are
 * typed
 *
 * @author Steve Ebersole
 */
public interface SqmTypedNode<T> extends SqmNode {
	/**
	 * The Java type descriptor for this node.
	 */
	default JavaTypeDescriptor<T> getNodeJavaTypeDescriptor() {
		final SqmExpressable<T> nodeType = getNodeType();
		return nodeType != null ? nodeType.getExpressableJavaTypeDescriptor() : null;
	}

	SqmExpressable<T> getNodeType();
}
