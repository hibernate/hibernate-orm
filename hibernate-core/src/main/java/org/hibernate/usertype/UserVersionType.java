/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;

import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * A user type that may be used for a version property
 *
 * @author Gavin King
 */
public interface UserVersionType<T> extends UserType<T>, Comparator<T> {
	/**
	 * Generate an initial version.
	 *
	 * @param session The session from which this request originates.  May be
	 * null; currently this only happens during startup when trying to determine
	 * the "unsaved value" of entities.
	 * @return an instance of the type
	 * @deprecated Use {@link #seed(WrapperOptions)} instead
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	T seed(SharedSessionContractImplementor session);

	/**
	 * Generate an initial version.
	 *
	 * @param options The options.
	 * @return an instance of the type
	 */
	default T seed(WrapperOptions options) {
		return seed( options.getSession() );
	}

	/**
	 * Increment the version.
	 *
	 * @param session The session from which this request originates.
	 * @param current the current version
	 * @return an instance of the type
	 * @deprecated Use {@link #next(Object, WrapperOptions)} instead
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	T next(T current, SharedSessionContractImplementor session);

	/**
	 * Increment the version.
	 *
	 * @param current the current version
	 * @param options The options.
	 * @return an instance of the type
	 */
	default T next(T current, WrapperOptions options) {
		return next( current, options.getSession() );
	}
}
