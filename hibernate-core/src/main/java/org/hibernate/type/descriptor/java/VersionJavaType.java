/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.SPI;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Additional Java-type contract for values used to version and
/// optimistically lock data.
///
/// @param <T> the represented version value type
/// @author Christian Beikov
@SPI({ USE, IMPLEMENT })
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
	 * @deprecated Use {@link #seed(Long, Integer, Integer, WrapperOptions)} instead
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	T seed(Long length, Integer precision, Integer scale, SharedSessionContractImplementor session);

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
	 * @param options The options.
	 * @return an instance of the type
	 */
	default T seed(Long length, Integer precision, Integer scale, WrapperOptions options) {
		return seed( length, precision, scale, options.getSession() );
	}

	/**
	 * Increment the version.
	 *
	 * @param current the current version
	 * @param length The length of the type
	 * @param precision The precision of the type
	 * @param scale The scale of the type
	 * @param session The session from which this request originates.
	 * @return an instance of the type
	 * @deprecated Use {@link #next(Object, Long, Integer, Integer, WrapperOptions)} instead
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	T next(T current, Long length, Integer precision, Integer scale, SharedSessionContractImplementor session);

	/**
	 * Increment the version.
	 *
	 * @param current the current version
	 * @param length The length of the type
	 * @param precision The precision of the type
	 * @param scale The scale of the type
	 * @param options The options.
	 * @return an instance of the type
	 */
	default T next(T current, Long length, Integer precision, Integer scale, WrapperOptions options) {
		return next( current, length, precision, scale, options.getSession() );
	}

}
