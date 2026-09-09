/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Remove;

/**
 * The contract for setting the value of a persistent attribute on its container/owner.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@Deprecated(since = "7.4", forRemoval = true)
@Remove // replace with a different SPI
public interface Setter extends Serializable {

	/**
	 * Set the property value on the given target instance.
	 *
	 * @param target The instance containing the property value to be set.
	 * @param value The value to be set.
	 *
	 * @deprecated Will be removed in 8.0
	 */
	@Deprecated(since = "7.4", forRemoval = true)
	void set(Object target, @Nullable Object value);

	/**
	 * Optional operation (may return {@code null})
	 */
	@Nullable String getMethodName();

	/**
	 * Optional operation (may return {@code null})
	 */
	@Nullable Method getMethod();
}
