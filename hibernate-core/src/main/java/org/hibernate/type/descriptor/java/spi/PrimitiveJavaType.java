/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;

import org.hibernate.type.descriptor.java.BasicJavaType;

/**
 * Additional contract for primitive / primitive wrapper Java types.
 *
 * @author Steve Ebersole
 */
public interface PrimitiveJavaType<J extends Serializable> extends BasicJavaType<J> {
	/**
	 * Retrieve the primitive counterpart to the wrapper type identified by
	 * this descriptor
	 *
	 * @return The primitive Java type.
	 */
	Class<?> getPrimitiveClass();

	/**
	 * Get the Java type that describes an array of this type.
	 */
	Class<J[]> getArrayClass();

	/**
	 * Get the Java type that describes an array of this type's primitive variant.
	 */
	Class<?> getPrimitiveArrayClass();
}
