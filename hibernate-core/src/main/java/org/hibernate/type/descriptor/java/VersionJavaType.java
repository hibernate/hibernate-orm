/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Additional contract for types which may be used to version (and optimistic lock) data.
 *
 * @author Christian Beikov
 */
public interface VersionJavaType<T> extends JavaType<T> {
	/**
	 * Generate an initial version.
	 * <p>
	 * Note that this operation is only used when the program sets a null or negative
	 * number as the value of the entity version field. It is not called when the
	 * program sets the version field to a sensible-looking version.
	 *
	 * @param length The length of the type
	 * @param precision The precision of the type
	 * @param scale The scale of the type
	 * @param session The session from which this request originates.
	 * @return an instance of the type
	 */
	T seed(Long length, Integer precision, Integer scale, SharedSessionContractImplementor session);

	/**
	 * Increment the version.
	 *
	 * @param current the current version
	 * @param length The length of the type
	 * @param precision The precision of the type
	 * @param scale The scale of the type
	 * @param session The session from which this request originates.
	 * @return an instance of the type
	 */
	T next(T current, Long length, Integer precision, Integer scale, SharedSessionContractImplementor session);

}
