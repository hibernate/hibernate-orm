/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.lang.reflect.Method;

import jakarta.annotation.Nullable;
import org.hibernate.Remove;

/**
 * The contract for setting the value of a persistent attribute on its container/owner.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@Deprecated(since = "7.4", forRemoval = true)
@Remove // replace with a different SPI
public interface Setter {

	/**
	 * Set the property value on the given target instance.
	 *
	 * @param target The instance containing the property value to be set.
	 * @param value The value to be set.
	 *
	 * @deprecated no longer used
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
