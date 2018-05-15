/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;

/**
 * Additional contract for primitive / primitive wrapper Java types.
 *
 * @author Steve Ebersole
 */
public interface Primitive<J extends Serializable> extends BasicJavaDescriptor<J> {
	/**
	 * Retrieve the primitive counterpart to the wrapper type identified by
	 * this descriptor
	 *
	 * @return The primitive Java type.
	 */
	Class getPrimitiveClass();

	/**`
	 * Get this Java type's default value.
	 *
	 * @return The default value.
	 */
	J getDefaultValue();
}
