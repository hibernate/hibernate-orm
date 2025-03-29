/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The contract for setting the value of a persistent attribute on its container/owner.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Setter extends Serializable {

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
