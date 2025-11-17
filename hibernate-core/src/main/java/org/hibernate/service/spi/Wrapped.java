/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import org.hibernate.service.UnknownUnwrapTypeException;

/**
 * Optional contract for services that wrap stuff that to which it is useful to have access.
 * <p>
 * For example, a service that maintains a {@link javax.sql.DataSource} might want to expose
 * access to the {@link javax.sql.DataSource} or its {@link java.sql.Connection} instances.
 *
 * @author Steve Ebersole
 */
public interface Wrapped {
	/**
	 * Can this wrapped service be unwrapped as the indicated type?
	 *
	 * @param unwrapType The type to check.
	 *
	 * @return True/false.
	 */
	boolean isUnwrappableAs(Class<?> unwrapType);

	/**
	 * Unproxy the service proxy
	 *
	 * @param unwrapType The java type as which to unwrap this instance.
	 *
	 * @return The unwrapped reference
	 *
	 * @throws UnknownUnwrapTypeException if the service cannot be unwrapped as the indicated type
	 */
	<T> T unwrap(Class<T> unwrapType);
}
